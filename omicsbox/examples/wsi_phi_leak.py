"""Quality check: non-empty patient-identifying attributes in DICOM WSI scans.

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

Run inside the sandbox with:
    curl -s -X POST --data-binary @examples/wsi_phi_leak.py http://localhost:8080/run
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
