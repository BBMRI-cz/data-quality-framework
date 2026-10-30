"""Quality check: DICOM WSI files missing mandatory metadata attributes.

Walks the mounted corpus, reads the header of every DICOM candidate file and
flags files where a Type-1 attribute of the VL Whole Slide Microscopy Image
IOD is absent or empty. VOLUME pyramid levels additionally require the total
pixel matrix dimensions and a frame count.

Result contract: a single JSON object printed as the LAST line of stdout:
    count (int):       number of affected files
    entityType (str):  always "file"
    ids (list[str]):   paths of the affected files, relative to the data dir
    error (str|null):  set by the script to describe a check-level error

Run inside the sandbox with:
    curl -s -X POST --data-binary @examples/wsi_metadata_completeness.py http://localhost:8080/run
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
