# Why Does My Report Contain N/A Values?

If your report contains `N/A`, the check did not produce a result during execution. At the moment, there are only two supported explanations:

1. The quality check failed because of invalid syntax or another execution error.
2. The database was unavailable when the check ran.

In both cases, the framework cannot calculate a value, so the report shows `N/A`.

## How to Troubleshoot

Use the following steps:

1. **Review the check definition** for syntax or execution errors.
2. **Check the agent and database logs** for failures or downtime around the execution time.
3. **Re-run the report** after fixing the check or restoring database availability.

## The Whole Report Is Marked FAILED

A report left in the `FAILED` state aborted during generation — for example because the database became unavailable mid-run, or because per-check epsilon budget preferences consumed the entire check budget, leaving no epsilon for the remaining checks (see [Privacy Configuration](../user/privacy-configuration.md#how-the-privacy-budget-is-spent)).

A failed report is never sent to the central server and is not retried automatically. Check the agent logs for the underlying error, fix the cause, and generate a new report.

## A Report Appears Stuck in GENERATING

Only one report can be generated at a time; requesting another one while a report is running returns a **409 Conflict** (shown as a notification in the UI). If the report page shows a report that stays in `GENERATING` for an unusually long time, its generation probably died mid-run.

The agent recovers automatically — reports stuck in `GENERATING` for more than **6 hours** are marked `FAILED`:

- on an hourly scheduled sweep,
- eagerly whenever a new report is requested,
- immediately at startup for reports left behind by an application restart (any in-process generation is provably dead after a restart).

Once the stuck report is marked `FAILED`, report generation works again — no restart required.

