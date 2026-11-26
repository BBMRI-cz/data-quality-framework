# Quality Checks

The **Quality Checks** page lists all data quality checks configured on your agent. Use the
**Category** filter to narrow the list to one category (or to uncategorized checks).

## Exporting Quality Checks

Click **Export** to download your quality checks as a JSON file (`quality-checks.json`). The
export respects the current category filter: with no filter selected you get every check, and
with a category selected you get only that category's checks.

Exporting is read-only — it never runs the check queries, never starts a report, and never
spends any differential privacy budget.

The exported file is portable and instance-independent: it contains no database IDs, no active
flags and no report data, so you can keep it in version control, use it as a backup, or import
it into another agent. Categories travel by **name**, not ID, because IDs differ between
instances.

## File Format

The file is a JSON array with one entry per quality check:

```json
[
  {
    "name": "Patients missing a birth date",
    "description": "Counts patients without a recorded birth date",
    "query": "SELECT count(*) FROM patient WHERE birth_date IS NULL",
    "type": "SQL",
    "warningThreshold": 10,
    "errorThreshold": 30,
    "epsilonBudget": 1.0,
    "category": "Completeness"
  }
]
```

| Field | Required | Description |
| --- | --- | --- |
| `name` | yes | Name of the check |
| `description` | no | What the check validates |
| `query` | yes | CQL/SQL query (or script) executed by the check |
| `type` | yes | Check type, e.g. `CQL`, `SQL`, `JAVA` or `PYTHON` |
| `warningThreshold` | yes | Percentage at which the check raises a warning |
| `errorThreshold` | yes | Percentage at which the check raises an error |
| `epsilonBudget` | no | Differential privacy epsilon per report; omitted when unset |
| `category` | no | Category name; omitted when the check is uncategorized |
