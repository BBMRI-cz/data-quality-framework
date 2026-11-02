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

The report stopped during generation, for example because the database became unavailable or a privacy setting was
invalid (see [Privacy Configuration](../privacy-configuration.md)). Check the agent logs for the error, fix the cause
and generate a new report. Failed reports are not retried automatically.

A failed report is normally not sent to the central server. If generation fails at the very last step, the server may
already have received it.

## A Report Appears Stuck in GENERATING

Only one report can run at a time. Requesting another one shows a "report generation in progress" message.

If a report stays in `GENERATING` for hours, its generation has most likely hung or crashed. The agent marks such
reports `FAILED` after 6 hours (it checks every hour, when a new report is requested, and at startup). New reports can
then run again.

If the report is still `GENERATING` after that, or new reports fail with a database error, restart the agent. A common
cause is a data store that stopped responding, so also check that the FHIR server or database is reachable.