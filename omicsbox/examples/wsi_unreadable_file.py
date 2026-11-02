"""Quality check: DICOM WSI files that cannot be parsed.

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

Run inside the sandbox with:
    curl -s -X POST --data-binary @examples/wsi_unreadable_file.py http://localhost:8080/run
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
