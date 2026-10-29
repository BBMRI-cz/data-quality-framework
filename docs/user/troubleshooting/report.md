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

