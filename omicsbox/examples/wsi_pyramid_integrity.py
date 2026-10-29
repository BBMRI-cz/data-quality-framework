"""Quality check: DICOM WSI pyramid levels with invalid structure.

Groups the VOLUME instances of every slide directory (one directory = one
scanned slide) into a resolution pyramid and flags slides whose pyramid is
unusable for tiled viewing:
  * fewer than two VOLUME levels (no zoom pyramid at all), or
  * levels that do not strictly decrease in total pixel matrix size
    (a resampled level larger than or equal to a higher-resolution level
    indicates a broken or re-exported pyramid).

Result contract: a single JSON object printed as the LAST line of stdout:
    count (int):       number of affected slide directories
    entityType (str):  always "file" (directories are file entities)
    ids (list[str]):   paths of the affected directories, relative to the data dir
    error (str|null):  set by the script to describe a check-level error

Run inside the sandbox with:
    curl -s -X POST --data-binary @examples/wsi_pyramid_integrity.py http://localhost:8080/run
"""

import json
import os

import pydicom

MIN_VOLUME_LEVELS = 2


def _volume_levels(directory, filenames):
    """(columns, rows) of every readable VOLUME instance in the directory."""
    levels = []
    for name in filenames:
        if not name.lower().endswith(".dcm"):
            continue
        try:
            ds = pydicom.dcmread(os.path.join(directory, name), stop_before_pixels=True)
        except Exception:
            continue  # unreadable files are reported by wsi_unreadable_file.py
        if "VOLUME" not in (ds.get("ImageType") or []):
            continue
        cols, rows = ds.get("TotalPixelMatrixColumns"), ds.get("TotalPixelMatrixRows")
        if cols and rows:
            levels.append((int(cols), int(rows)))
    return levels


def _is_strictly_pyramidal(levels) -> bool:
    ordered = sorted(levels, key=lambda level: level[0] * level[1], reverse=True)
    return all(
        high[0] > low[0] and high[1] > low[1]
        for high, low in zip(ordered, ordered[1:], strict=False)
    )


def main() -> None:
    data_dir = os.getcwd()  # the sandbox runs scripts with cwd = data dir
    flagged = []
    for root, _dirs, files in os.walk(data_dir):
        levels = _volume_levels(root, files)
        if not levels:
            continue  # no WSI pyramid in this directory
        if len(levels) < MIN_VOLUME_LEVELS or not _is_strictly_pyramidal(levels):
            flagged.append(os.path.relpath(root, data_dir))
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
    main()
