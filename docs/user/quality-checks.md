# Quality Checks

The **Quality Checks** page lists all data quality checks configured on your agent. Use the
**Category** filter to narrow the list to one category (or to uncategorized checks).

## Exporting Quality Checks

Click **Export** to download your quality checks as a JSON file (`quality-checks.json`). The
export respects the current category filter: with no filter selected you get every check, and
with a category selected you get only that category's checks. The API behind the button also
accepts several `categoryName` parameters at once, which are combined with OR, while the page
itself always exports the currently selected category.

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
| `description` | yes | What the check validates |
| `query` | yes | CQL/SQL query (or script) executed by the check |
| `type` | yes | Check type, e.g. `CQL`, `SQL`, `JAVA` or `PYTHON` |
| `warningThreshold` | yes | Percentage at which the check raises a warning |
| `errorThreshold` | yes | Percentage at which the check raises an error |
| `epsilonBudget` | no | Differential privacy epsilon per report; omitted when unset |
| `category` | no | Category name; omitted when the check is uncategorized |

## Importing Quality Checks

Click **Import** on the Quality Checks page and choose a JSON file in the format shown above —
any file produced by **Export** imports unchanged. The API behind the button is
`POST /api/quality-checks/import` with the JSON array as its request body. Like all quality
check APIs it requires an authenticated user.

Importing only writes check definitions — it never runs a query, never starts a report and never
spends any differential privacy budget. The response reports the counts of created, updated and
unchanged checks.

Import rules:

- Checks are matched by exact **name**: new names are created, existing ones are updated only
  when a field differs, so re-importing the same file changes nothing. The enabled/disabled
  state of existing checks is never touched.
- `category` is a category **name**. Existing categories are matched by name; missing ones are
  created without a color (edit them later to assign one).
- The whole file is validated before anything is saved: if any entry is invalid, the import is
  rejected with an error naming the offending entries and nothing is written.
- Built-in `JAVA` checks are identified by name and can only be updated, never created; their
  `query` is implemented in the agent, so the `query` field is ignored for them. An entry with
  another type cannot replace a built-in check of the same name.
- An absent `type` defaults to `CQL`.

Note that renaming a built-in `JAVA` check on one agent makes its exported name unmatchable on
another agent that still has the original name; the import then rejects that entry with a clear
error and saves nothing. Checks whose name occurs more than once — in the file or among the
existing checks — are rejected as well, since the name is used for matching.
