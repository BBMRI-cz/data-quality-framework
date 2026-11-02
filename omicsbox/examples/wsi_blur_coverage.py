"""Quality check: WSI slides with out-of-focus (blurred) tissue.

Executable port of RationAI's ``blur_score_piqe``
(https://github.com/RationAI/quality-control/blob/master/rationai/qc/blur/blur_score_piqe.py)
to whole-slide images read with OpenSlide (.mrxs, .svs, .ndpi, ...). Blur is
detected per 16x16 px block with the PIQE perception approach: after median
filtering (kernel derived from the pixel size) the image is MSCN-normalized
and blocks whose variance stays below the activity threshold count as
blurred — just like the original, which discards the scalar PIQE score and
keeps only the PIQE activity mask. Areas without tissue are suppressed with
a simple foreground mask, so background does not inflate the result. A slide
is flagged when blurred blocks cover at least ``BLUR_COVERAGE_THRESHOLD`` of
its sampled tissue; out-of-focus tissue is useless for pathologists and
algorithms alike, so such slides may need re-scanning.

Since a WSI at 40x magnification is far too large to analyze as a whole, the
check samples: a coarse whole-slide thumbnail localizes the tissue (tissue
often covers a fraction of the glass, so blind geometric sampling would miss
it), and up to ``MAX_PROBED_TILES`` tiles of ``TILE_SIZE`` px are probed,
evenly spread over the tissue tiles, at the pyramid level closest to
~0.44 um/px (where the score is most accurate). Blur coverage is aggregated
across the probed tiles. Slides are analyzed by ``WORKERS`` parallel worker
processes (one CPU core each) to keep bulk runs over large corpora feasible.

Corpus layout: every single-file slide OpenSlide can read is a candidate.
A Mirax scan consists of an ``<SCAN>.mrxs`` index file and a companion
directory ``<SCAN>/`` holding the tile data, with ``SCAN`` typically of the
form ``P<patient>-S<scan>`` (e.g. ``P00040-S00064``). The check reports the
scan identifier — the slide path relative to the data dir, without the file
extension — so one id identifies the patient-scan and points at both the
.mrxs file and its companion directory. Directories listed in
``EXCLUDE_DIRS`` (e.g. the local ``anonymization`` export folder) are never
scanned. Slides that cannot be opened or analyzed are skipped with a note on
stderr; the corpus is not failed because of a single bad scan, while a
check-level failure still emits the JSON with ``error`` set (and a traceback
on stderr). Set ``MAX_SLIDES`` to analyze only the first N scans (sorted by
scan id, so the subset is deterministic) — handy for pilot runs before
checking a full corpus.

Result contract: a single JSON object printed as the LAST line of stdout:
    count (int):       number of affected scans
    entityType (str):  always "file" (a scan is a .mrxs file + companion dir)
    ids (list[str]):   scan identifiers of the affected slides (see above)
    error (str|null):  set by the script to describe a check-level error

Run inside the sandbox with:
    curl -s -X POST --data-binary @examples/wsi_blur_coverage.py http://localhost:8080/run
Timing: roughly ~15-25 s per slide per worker with the defaults (14 probed
tiles x ~1.4 s decode+analysis plus the thumbnail pass), i.e. about 1-1.5 h
for 600 slides on 3 worker cores; size OMICSBOX_TIMEOUT (default 60 s)
accordingly. Note that /run is a single synchronous HTTP call — for very
large corpora, prefer batching the mount into several smaller runs over one
multi-hour request. Memory: ~1.5 GB total for 3 workers.
"""

import json
import math
import os
import sys

import cv2
import numpy as np
import openslide
from skimage.color import rgb2gray
from skimage.filters import median
from skimage.morphology import area_opening, erosion

# --- Tunables ---------------------------------------------------------------

OPTIMAL_PIXEL_SIZE_UM = 0.44  # PIQE is most accurate around 40x magnification
BLOCK_SIZE = 16  # PIQE activity blocks (px)
ACTIVITY_THRESHOLD = 0.1  # min MSCN block variance of a "sharp" block
FOREGROUND_THRESHOLD = 0.92  # ~235/255: gray values below count as tissue
TILE_SIZE = 1024  # probe tiles, multiple of BLOCK_SIZE
MAX_PROBED_TILES = 16  # per-slide probe budget, evenly spread over TISSUE tiles
MIN_FOREGROUND_FRACTION = 0.25  # grid positions below this tissue share are ignored
BLUR_COVERAGE_THRESHOLD = 0.2  # flag scans with >= 20% sampled tissue blurred
THUMB_TILE_PIXELS = 8  # thumbnail resolution per candidate probe tile
WORKERS = 3  # worker processes analyzing slides in parallel (~1 CPU core each)
MAX_SLIDES = None  # analyze only the first N scans (sorted by scan id); None = all
EXCLUDE_DIRS = ("anonymization",)  # directory subtrees never scanned for slides

SLIDE_SUFFIXES = (".mrxs", ".svs", ".ndpi", ".vms", ".vmu", ".scn", ".bif")

# --- Blur scoring (self-contained equivalent of rationai.qc.blur) -----------


def _calculate_mscn(image):
    """MSCN coefficients, as in the PIQE reference implementation."""
    image = image.astype(np.float32)
    mu = cv2.GaussianBlur(image, (7, 7), 7 / 6)
    sigma_sq = cv2.GaussianBlur(image**2, (7, 7), 7 / 6) - mu * mu
    sigma = np.sqrt(np.abs(sigma_sq))
    return (image - mu) / (1 + sigma)


def _activity_mask(image_255):
    """Per-pixel mask of PIQE "high spatial activity" blocks (True = sharp).

    Vectorized equivalent of PIQE's ActivityMask: the image is edge-padded to
    a multiple of BLOCK_SIZE (before MSCN, as in PIQE) and every 16x16 block
    with variance above the activity threshold is marked active. Unlike the
    reference piqe.py (which crops the mask one block column off), the mask is
    cropped back to the exact image shape; probed tiles are multiples of
    BLOCK_SIZE, so there the implementations match exactly.
    """
    height, width = image_255.shape
    pad_h = (-height) % BLOCK_SIZE
    pad_w = (-width) % BLOCK_SIZE
    padded = np.pad(image_255, ((0, pad_h), (0, pad_w)), mode="edge")
    mscn = _calculate_mscn(padded)
    rows, cols = mscn.shape[0] // BLOCK_SIZE, mscn.shape[1] // BLOCK_SIZE
    blocks = mscn.reshape(rows, BLOCK_SIZE, cols, BLOCK_SIZE)
    active_blocks = blocks.var(axis=(1, 3)) > ACTIVITY_THRESHOLD
    mask = np.repeat(np.repeat(active_blocks, BLOCK_SIZE, axis=0), BLOCK_SIZE, axis=1)
    return mask[:height, :width]


def _foreground_mask(gray):
    """Binary tissue mask: dark enough, de-speckled, eroded — as in the original."""
    foreground = gray < FOREGROUND_THRESHOLD
    foreground = area_opening(foreground)
    # binary erosion with a 5x5 footprint ("erosion" is the non-deprecated name)
    return erosion(foreground, footprint=np.ones((5, 5))).astype(bool)


def _rgb_on_white(region):
    """RGB array of a PIL region with transparent pixels composited onto white.

    OpenSlide renders areas without data (e.g. around rotated scan strips) as
    transparent; compositing onto white treats them as background instead of
    dark, activity-free "tissue".
    """
    rgba = np.asarray(region.convert("RGBA")).astype(np.uint16)
    alpha = rgba[..., 3:4]
    return ((rgba[..., :3] * alpha + 255 * (255 - alpha)) // 255).astype(np.uint8)


def _tile_blur_and_tissue(tile, pixel_size_um):
    """(blurred tissue px, tissue px) of one RGB tile, or None if too small."""
    height, width = tile.shape[:2]
    if height < BLOCK_SIZE or width < BLOCK_SIZE:
        return None
    kernel_size = 2 * round(1 / pixel_size_um * OPTIMAL_PIXEL_SIZE_UM + 1e-9) + 1
    gray = median(rgb2gray(tile), footprint=np.ones((kernel_size, kernel_size), dtype=bool))
    foreground = _foreground_mask(gray)
    if not foreground.any():
        return None
    sharp = _activity_mask(gray * 255)
    return int((~sharp & foreground).sum()), int(foreground.sum())


# --- Reading the slide ------------------------------------------------------


def _closest_level(spacings_um):
    """Index of the level whose pixel size is closest to the optimal one."""
    return min(range(len(spacings_um)), key=lambda i: abs(math.log(spacings_um[i])))


def _tissue_tile_positions(slide, width, height):
    """Grid positions of TILE_SIZE tiles whose estimated tissue share qualifies.

    The estimate comes from a coarse whole-slide thumbnail (a few hundred px):
    WSI tissue usually covers a small part of the glass, so probing tiles at
    geometric strides blindly would often land on empty glass. Only grid
    positions whose thumbnail-mapped tissue fraction reaches
    MIN_FOREGROUND_FRACTION are returned, in reading order.
    """
    cols = list(range(0, width - TILE_SIZE + 1, TILE_SIZE))
    rows = list(range(0, height - TILE_SIZE + 1, TILE_SIZE))
    if not cols or not rows:
        return []
    thumb = _rgb_on_white(
        slide.get_thumbnail((len(cols) * THUMB_TILE_PIXELS, len(rows) * THUMB_TILE_PIXELS))
    )
    foreground = rgb2gray(thumb) < FOREGROUND_THRESHOLD
    foreground = area_opening(foreground).astype(np.float32)
    per_tile = cv2.resize(foreground, (len(cols), len(rows)), interpolation=cv2.INTER_AREA)
    return [
        (x, y)
        for j, y in enumerate(rows)
        for i, x in enumerate(cols)
        if per_tile[j, i] >= MIN_FOREGROUND_FRACTION
    ]


def _blur_coverage(slide):
    """Blur coverage (0.0-1.0) over probed tissue tiles, or None when unscorable."""
    base_mpp = float(slide.properties.get("openslide.mpp-x") or 0)
    if base_mpp <= 0:
        print("missing openslide.mpp-x, skipped", file=sys.stderr)
        return None
    spacings_um = [base_mpp * slide.level_downsamples[i] for i in range(slide.level_count)]
    level = _closest_level([spacing / OPTIMAL_PIXEL_SIZE_UM for spacing in spacings_um])
    pixel_size_um = spacings_um[level]
    downsample = round(slide.level_downsamples[level])
    width, height = slide.level_dimensions[level]
    if width < TILE_SIZE or height < TILE_SIZE:
        probes = [(0, 0, width, height)]
    else:
        positions = _tissue_tile_positions(slide, width, height)
        step = max(1, math.ceil(len(positions) / MAX_PROBED_TILES))
        probes = [(x, y, TILE_SIZE, TILE_SIZE) for x, y in positions[::step]]
    blurred = tissue = 0
    for x, y, w, h in probes:
        # OpenSlide locations are in level-0 coordinates
        try:
            region = slide.read_region((x * downsample, y * downsample), level, (w, h))
            result = _tile_blur_and_tissue(_rgb_on_white(region), pixel_size_um)
        except Exception as e:
            print(f"tile ({x},{y}) unreadable ({e}), ignored", file=sys.stderr)
            continue
        if result is None:
            continue
        tile_blurred, tile_tissue = result
        if tile_tissue < MIN_FOREGROUND_FRACTION * w * h:
            continue  # thumbnail estimate lied (e.g. folded tissue); ignore the tile
        blurred += tile_blurred
        tissue += tile_tissue
    if not tissue:
        print("no tissue sampled in probe tiles, skipped", file=sys.stderr)
        return None
    return blurred / tissue


def _scan_coverage(scan):
    """(scan_id, coverage) of one slide; runs inside a worker process.

    A scan must never take the whole run down with it: any failure is
    reported on stderr and yields a None coverage (unscored, not flagged).
    """
    scan_id, path = scan
    cv2.setNumThreads(1)  # N workers, one core each: no thread oversubscription
    print(f"{scan_id}: analyzing...", file=sys.stderr)
    try:
        slide = openslide.OpenSlide(path)
    except Exception as e:
        print(f"{scan_id}: cannot be opened ({e}), skipped", file=sys.stderr)
        return scan_id, None
    try:
        return scan_id, _blur_coverage(slide)
    except Exception as e:
        print(f"{scan_id}: analysis failed ({e}), skipped", file=sys.stderr)
        return scan_id, None
    finally:
        slide.close()


# --- Scan discovery ---------------------------------------------------------


def _iter_scans(data_dir):
    """Yield (scan_id, path) for every slide file under data_dir.

    ``scan_id`` is the path relative to the data dir without the file
    extension (e.g. ``mirax/P00040-S00064``); for Mirax it identifies both
    the .mrxs index file and the companion tile directory of the same name.
    """
    for root, dirs, files in os.walk(data_dir):
        dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]  # prune excluded subtrees
        for name in files:
            if name.lower().endswith(SLIDE_SUFFIXES):
                path = os.path.join(root, name)
                yield os.path.splitext(os.path.relpath(path, data_dir))[0], path


def main() -> None:
    import multiprocessing

    data_dir = os.getcwd()  # the sandbox runs scripts with cwd = data dir
    scans = sorted(_iter_scans(data_dir))
    if MAX_SLIDES is not None:
        scans = scans[:MAX_SLIDES]
    print(f"{len(scans)} scan(s) to analyze with {WORKERS} worker(s)", file=sys.stderr)
    if WORKERS > 1 and len(scans) > 1:
        # "fork" workers inherit this script; no re-import, no exec from the
        # noexec tmpfs. pool.map keeps input order, so output is deterministic.
        with multiprocessing.get_context("fork").Pool(WORKERS) as pool:
            results = pool.map(_scan_coverage, scans)
    else:
        results = [_scan_coverage(scan) for scan in scans]
    flagged = []
    for i, (scan_id, coverage) in enumerate(results, 1):
        if coverage is None:
            continue
        print(
            f"[{i}/{len(results)}] {scan_id}: blur coverage {coverage:.1%} of sampled tissue",
            file=sys.stderr,
        )
        if coverage >= BLUR_COVERAGE_THRESHOLD:
            flagged.append(scan_id)
    print(
        json.dumps(
            {
                "count": len(flagged),
                "entityType": "file",
                "ids": sorted(flagged),
                "error": None,
            }
        )
    )


if __name__ == "__main__":
    try:
        main()
    except Exception as e:  # a check-level failure must still honor the contract
        import traceback

        traceback.print_exc()  # full details on stderr
        print(
            json.dumps(
                {
                    "count": 0,
                    "entityType": "file",
                    "ids": [],
                    "error": f"check failed: {e}",
                }
            )
        )
        sys.exit(1)
