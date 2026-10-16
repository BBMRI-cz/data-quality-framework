"""Example data quality check for the omicsbox sandbox.

Flags every data file in the mounted corpus that lacks a companion metadata
file (``<name>.json`` next to it) and reports the result in the omicsbox
result contract: a single JSON object printed as the LAST line of stdout.

Contract fields:
    count (int):       number of affected entities (the raw check result)
    entityType (str):  what was counted; the sandbox supports "file"
    ids (list[str]):   identifiers of the affected entities
    error (str|null):  set by the script to describe a check-level error

A non-zero exit code or malformed/absent JSON makes the calling agent treat
the check as failed, so always emit the JSON document exactly once and as
the last line of stdout.

Run inside the sandbox with:
    curl -s -X POST --data-binary @examples/example_check.py http://localhost:8080/run
"""

import json
import os


def main() -> None:
    data_dir = os.getcwd()  # the sandbox runs scripts with cwd = data dir
    missing = []
    for root, _dirs, files in os.walk(data_dir):
        for name in files:
            if name.endswith(".json"):
                continue
            path = os.path.join(root, name)
            if not os.path.exists(path + ".json"):
                missing.append(os.path.relpath(path, data_dir))
    print(
        json.dumps(
            {
                "count": len(missing),
                "entityType": "file",
                "ids": sorted(missing),
                "error": None,
            }
        )
    )


if __name__ == "__main__":
    main()
