/**
 * Utility functions for report result handling
 */

export const CHECK_STATUS = {
  PASSED: 'PASSED',
  WARNING: 'WARNING',
  FAILED: 'FAILED',
};

/**
 * Get the effective result value.
 * Prefers raw value, then obfuscated value.
 *
 * @param {Object} result - Report result
 * @returns {number|null} Numeric result value or null when missing
 */
export function getOccurrenceValue(result) {
  if (result?.rawValue != null) {
    const rawValue = Number(result.rawValue);
    if (Number.isFinite(rawValue)) {
      return rawValue;
    }
  }

  if (result?.obfuscatedValue != null) {
    const obfuscatedValue = Number(result.obfuscatedValue);
    if (Number.isFinite(obfuscatedValue)) {
      return obfuscatedValue;
    }
  }

  return null;
}

/**
 * Calculate the occurrence rate as a percentage string without the trailing `%` symbol.
 *
 * @param {Object} report - Report object containing numberOfEntities
 * @param {Object} result - Report result
 * @returns {string} Percentage string or `N/A`
 */
export function calculatePercentage(report, result) {
  const total = Number(report?.numberOfEntities);
  if (!Number.isFinite(total) || total <= 0) {
    return 'N/A';
  }

  const occurrenceValue = getOccurrenceValue(result);
  if (!Number.isFinite(occurrenceValue)) {
    return 'N/A';
  }

  return ((occurrenceValue / total) * 100).toFixed(2);
}

/**
 * Format the occurrence rate for display.
 *
 * @param {Object} report - Report object containing numberOfEntities
 * @param {Object} result - Report result
 * @returns {string} Formatted occurrence rate for display
 */
export function formatOccurrenceRate(report, result) {
  const percentage = calculatePercentage(report, result);
  return percentage === 'N/A' ? percentage : `${percentage}%`;
}

/**
 * Get the status of a report result.
 * Null or missing values are treated as failures.
 *
 * @param {Object} report - Report object containing numberOfEntities
 * @param {Object} result - Report result
 * @returns {string} Check status
 */
export function getResultStatus(report, result) {
  if (result?.error) {
    return CHECK_STATUS.FAILED;
  }

  const occurrenceValue = getOccurrenceValue(result);
  if (!Number.isFinite(occurrenceValue)) {
    return CHECK_STATUS.FAILED;
  }

  const percentage = Number(calculatePercentage(report, result));
  if (!Number.isFinite(percentage)) {
    return CHECK_STATUS.FAILED;
  }

  if (percentage >= result.errorThreshold) {
    return CHECK_STATUS.FAILED;
  }

  if (percentage >= result.warningThreshold) {
    return CHECK_STATUS.WARNING;
  }

  return CHECK_STATUS.PASSED;
}

/**
 * Get the Bootstrap class for a report result status.
 *
 * @param {Object} report - Report object containing numberOfEntities
 * @param {Object} result - Report result
 * @returns {string} Bootstrap background class
 */
export function getResultClass(report, result) {
  const status = getResultStatus(report, result);
  if (status === CHECK_STATUS.FAILED) {
    return 'bg-danger';
  }
  if (status === CHECK_STATUS.WARNING) {
    return 'bg-warning';
  }
  return 'bg-success';
}

/**
 * Get sort priority for a report result.
 *
 * @param {Object} report - Report object containing numberOfEntities
 * @param {Object} result - Report result
 * @returns {number} Sort priority
 */
export function getResultPriority(report, result) {
  const status = getResultStatus(report, result);
  if (status === CHECK_STATUS.FAILED) {
    return 0;
  }

  if (status === CHECK_STATUS.WARNING) {
    return 1;
  }

  return 2;
}

/**
 * Get an error message for a failed result.
 *
 * @param {Object} report - Report object containing numberOfEntities
 * @param {Object} result - Report result
 * @returns {string|null} Error message or null when none should be shown
 */
export function getResultError(report, result) {
  if (result?.error) {
    return result.error;
  }

  return getResultStatus(report, result) === CHECK_STATUS.FAILED && !Number.isFinite(getOccurrenceValue(result))
    ? 'There is no error message'
    : null;
}

/**
 * Count results by status.
 *
 * @param {Object} report - Report object
 * @returns {{passed:number, warnings:number, failed:number}} Counts by status
 */
export function countResultsByStatus(report) {
  const counts = {
    passed: 0,
    warnings: 0,
    failed: 0,
  };

  if (!report?.results || !Array.isArray(report.results)) {
    return counts;
  }

  report.results.forEach((result) => {
    const status = getResultStatus(report, result);
    if (status === CHECK_STATUS.PASSED) {
      counts.passed++;
    } else if (status === CHECK_STATUS.WARNING) {
      counts.warnings++;
    } else if (status === CHECK_STATUS.FAILED) {
      counts.failed++;
    }
  });

  return counts;
}

/**
 * Get a summary of report result counts.
 *
 * @param {Object} report - Report object
 * @returns {{total:number, passed:number, warnings:number, failed:number}} Summary counts
 */
export function getResultSummary(report) {
  const counts = countResultsByStatus(report);

  return {
    total: Array.isArray(report?.results) ? report.results.length : 0,
    passed: counts.passed,
    warnings: counts.warnings,
    failed: counts.failed,
  };
}

