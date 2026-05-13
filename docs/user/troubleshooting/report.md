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


