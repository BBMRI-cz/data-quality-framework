-- OmicsBox Python quality checks for the mounted WSI corpus (see omicsbox/examples/)
-- NOTE: this feature is still under development; these checks seed development data only.
INSERT OR REPLACE INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold)
VALUES (9001, 'WSI: Missing mandatory DICOM attributes',
        'How many DICOM WSI files lack Type 1 metadata attributes (SOP/Study/Series UIDs, Modality, total pixel matrix dimensions, number of frames)',
        '"""Quality check: DICOM WSI files missing mandatory metadata attributes.

Walks the mounted corpus, reads the header of every DICOM candidate file and
flags files where a Type-1 attribute of the VL Whole Slide Microscopy Image
IOD is absent or empty. VOLUME pyramid levels additionally require the total
pixel matrix dimensions and a frame count.

Result contract: a single JSON object printed as the LAST line of stdout:
    count (int):       number of affected files
    entityType (str):  always "file"
    ids (list[str]):   paths of the affected files, relative to the data dir
    error (str|null):  set by the script to describe a check-level error
"""

import json
import os

import pydicom

# Mandatory (Type 1) attributes every VL WSI instance must carry.
REQUIRED_BASE = (
    "SOPClassUID",
    "SOPInstanceUID",
    "StudyInstanceUID",
    "SeriesInstanceUID",
    "Modality",
)

# Additional Type 1 attributes of tiled multi-frame VOLUME pyramid levels:
# without them the slide cannot be reconstructed or viewed.
REQUIRED_VOLUME = (
    "TotalPixelMatrixColumns",
    "TotalPixelMatrixRows",
    "NumberOfFrames",
)


def _missing_attributes(ds, required):
    return [name for name in required if not getattr(ds, name, None)]


def main() -> None:
    data_dir = os.getcwd()  # the sandbox runs scripts with cwd = data dir
    flagged = []
    for root, _dirs, files in os.walk(data_dir):
        for name in files:
            if not name.lower().endswith(".dcm"):
                continue
            path = os.path.join(root, name)
            try:
                ds = pydicom.dcmread(path, stop_before_pixels=True)
            except Exception:
                # Unreadable files are reported by wsi_unreadable_file.py.
                continue
            missing = _missing_attributes(ds, REQUIRED_BASE)
            if "VOLUME" in (ds.get("ImageType") or []):
                missing += _missing_attributes(ds, REQUIRED_VOLUME)
            if missing:
                flagged.append(os.path.relpath(path, data_dir))
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
',
        'PYTHON', 1, 10);

INSERT OR REPLACE INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold)
VALUES (9002, 'WSI: Non-empty patient-identifying attributes',
        'How many DICOM files carry any non-empty value in a patient-identifying attribute of the DICOM Basic Confidentiality Profile (a heuristic audit: hits may be valid pseudonyms, so review them against the site pseudonymization policy)',
        '"""Quality check: non-empty patient-identifying attributes in DICOM WSI scans.

Reports every DICOM file in the corpus that still carries a non-empty value
in a patient-identifying attribute (basic profile of DICOM de-identification).
This is a heuristic audit, not a pseudonymization verdict: a valid replacement
value (e.g. PatientID="anon-001") also satisfies the predicate and is
reported. Review every hit against the site pseudonymization policy before
treating it as a privacy leak.

Result contract: a single JSON object printed as the LAST line of stdout:
    count (int):       number of affected files
    entityType (str):  always "file"
    ids (list[str]):   paths of the affected files, relative to the data dir
    error (str|null):  set by the script to describe a check-level error
"""

import json
import os

import pydicom

# Identifiers from the DICOM Basic Application Level Confidentiality Profile
# that should be reviewed (empty or pseudonymized) before a slide leaves the
# provider. A non-empty value is not automatically a leak.
PHI_TAGS = (
    "PatientName",
    "PatientID",
    "PatientBirthDate",
    "PatientSex",
    "PatientAddress",
    "OtherPatientIDs",
    "AccessionNumber",
    "ReferringPhysicianName",
    "InstitutionName",
    "InstitutionAddress",
    "OperatorsName",
    "PerformingPhysicianName",
    "StudyDate",
    "SeriesDate",
)


def main() -> None:
    data_dir = os.getcwd()  # the sandbox runs scripts with cwd = data dir
    flagged = []
    for root, _dirs, files in os.walk(data_dir):
        for name in files:
            if not name.lower().endswith(".dcm"):
                continue
            path = os.path.join(root, name)
            try:
                ds = pydicom.dcmread(path, stop_before_pixels=True)
            except Exception:
                # Unreadable files are reported by wsi_unreadable_file.py.
                continue
            leaks = [tag for tag in PHI_TAGS if getattr(ds, tag, None) not in (None, "")]
            if leaks:
                flagged.append(os.path.relpath(path, data_dir))
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
',
        'PYTHON', 1, 10);

INSERT OR REPLACE INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold)
VALUES (9003, 'WSI: Invalid pyramid structure',
        'How many slide directories have an unusable WSI pyramid (fewer than two VOLUME levels or non-monotonically decreasing level sizes)',
        '"""Quality check: DICOM WSI pyramid levels with invalid structure.

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
',
        'PYTHON', 1, 10);

INSERT OR REPLACE INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold)
VALUES (9004, 'WSI: Unreadable DICOM file',
        'How many .dcm files cannot be parsed as DICOM Part 10 or lack pixel data (truncated transfers, corrupted exports)',
        '"""Quality check: DICOM WSI files that cannot be parsed.

Flags every ``.dcm`` candidate in the corpus whose metadata header cannot be
parsed as a DICOM Part 10 file, or that lacks pixel data entirely — typically
truncated transfers, non-DICOM files renamed to ``.dcm``, or corrupted
exports. Large element values (including the pixel data itself) are deferred,
so the check stays cheap even for multi-gigabyte pyramid levels.

Result contract: a single JSON object printed as the LAST line of stdout:
    count (int):       number of affected files
    entityType (str):  always "file"
    ids (list[str]):   paths of the affected files, relative to the data dir
    error (str|null):  set by the script to describe a check-level error
"""

import json
import os

import pydicom


def main() -> None:
    data_dir = os.getcwd()  # the sandbox runs scripts with cwd = data dir
    flagged = []
    for root, _dirs, files in os.walk(data_dir):
        for name in files:
            if not name.lower().endswith(".dcm"):
                continue
            path = os.path.join(root, name)
            try:
                ds = pydicom.dcmread(path, defer_size="1 KB")
                if "PixelData" not in ds:
                    flagged.append(os.path.relpath(path, data_dir))
            except Exception:
                flagged.append(os.path.relpath(path, data_dir))
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
',
        'PYTHON', 1, 10);
