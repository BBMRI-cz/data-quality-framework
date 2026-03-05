/**
 * Utility functions for working with quality checks and results
 */

/**
 * Check status enumeration
 */
export const CheckStatus = {
  PASSED: 'PASSED',
  WARNING: 'WARNING',
  FAILED: 'FAILED',
  UNKNOWN: 'UNKNOWN',
  NO_DATA: 'NO DATA',
};

/**
 * Get the status of a quality check result
 * Supports result values expressed either as fraction (0-1) or percentage (0-100).
 * Higher values indicate worse quality.
 */
export function getCheckStatus(result, check) {
  if (!check) return CheckStatus.UNKNOWN;
  if (result == null || typeof result.result !== 'number') return CheckStatus.UNKNOWN;

  const errorThreshold = check.errorThreshold;
  const warningThreshold = check.warningThreshold;

  // Normalize: if value looks like a fraction (<=1), convert to percentage.
  const raw = result.result;
  const percentage = raw <= 1 ? raw * 100 : raw;

  if (percentage > errorThreshold) {
    return CheckStatus.FAILED;
  } else if (percentage > warningThreshold) {
    return CheckStatus.WARNING;
  } else {
    return CheckStatus.PASSED;
  }
}

/**
 * Get the badge class for a check status
 * @param {string} status - The check status
 * @returns {string} Bootstrap badge class
 */
export function getStatusBadgeClass(status) {
  switch (status) {
    case CheckStatus.PASSED:
      return 'bg-success';
    case CheckStatus.FAILED:
      return 'bg-danger';
    case CheckStatus.WARNING:
      return 'bg-warning text-dark';
    default:
      return 'bg-secondary';
  }
}

/**
 * Get the color class for a progress indicator
 * @param {Object} result - The check result object
 * @param {Object} check - The quality check definition
 * @returns {string} CSS classes for progress color
 */
export function getProgressColorClass(result, check) {
  if (!check) return 'border-secondary bg-secondary';

  const status = getCheckStatus(result, check);

  switch (status) {
    case CheckStatus.FAILED:
      return 'border-danger bg-danger';
    case CheckStatus.WARNING:
      return 'border-warning bg-warning';
    case CheckStatus.PASSED:
      return 'border-success bg-success';
    default:
      return 'border-secondary bg-secondary';
  }
}

/**
 * Count checks by status in a report
 * @param {Object} report - The report object
 * @param {Map} qualityCheckMap - Map of check hash -> check definition
 * @returns {Object} Counts object with passed, warning, and failed properties
 */
export function countChecksByStatus(report, qualityCheckMap) {
  const counts = {
    passed: 0,
    warnings: 0,
    failed: 0,
    total: 0,
  };

  if (!report.results || !Array.isArray(report.results)) {
    return counts;
  }

  counts.total = report.results.length;

  report.results.forEach((result) => {
    const check = qualityCheckMap.get(result.hash);
    if (!check) return;

    const status = getCheckStatus(result, check);

    switch (status) {
      case CheckStatus.PASSED:
        counts.passed++;
        break;
      case CheckStatus.WARNING:
        counts.warnings++;
        break;
      case CheckStatus.FAILED:
        counts.failed++;
        break;
    }
  });

  return counts;
}

/**
 * Get the overall status of a report
 * @param {Object} report - The report object
 * @param {Map} qualityCheckMap - Map of check hash -> check definition
 * @returns {string} Overall report status
 */
export function getReportStatus(report, qualityCheckMap) {
  if (!report.results || report.results.length === 0) {
    return CheckStatus.NO_DATA;
  }

  let hasError = false;
  let hasWarning = false;

  report.results.forEach((result) => {
    const check = qualityCheckMap.get(result.hash);
    if (!check) return;

    const status = getCheckStatus(result, check);

    if (status === CheckStatus.FAILED) {
      hasError = true;
    } else if (status === CheckStatus.WARNING) {
      hasWarning = true;
    }
  });

  if (hasError) return CheckStatus.FAILED;
  if (hasWarning) return CheckStatus.WARNING;
  return CheckStatus.PASSED;
}

/**
 * Format a score as a percentage
 * @param {number} score - The score value (0-1)
 * @returns {string} Formatted percentage string
 */
export function formatScore(score) {
  return (score * 100).toFixed(1) + '%';
}

/**
 * Format a score as a rounded percentage for display
 * @param {number} score - The score value (0-1)
 * @returns {number} Rounded percentage
 */
export function formatScoreRounded(score) {
  return Math.round(score * 100);
}

/**
 * Get the Bootstrap icon class for a check status
 * @param {string} status - The check status (PASSED, WARNING, FAILED, etc.)
 * @param {boolean} filled - Whether to use filled icon variant (default: true)
 * @returns {string} Bootstrap icon class name
 */
export function getStatusIcon(status, filled = true) {
  const suffix = filled ? '-fill' : '';

  switch (status) {
    case CheckStatus.PASSED:
      return `bi bi-check-circle${suffix}`;
    case CheckStatus.WARNING:
      return `bi bi-exclamation-circle${suffix}`;
    case CheckStatus.FAILED:
      return `bi bi-exclamation-triangle${suffix}`;
    case CheckStatus.NO_DATA:
      return 'bi bi-question-circle';
    case CheckStatus.UNKNOWN:
    default:
      return 'bi bi-question-circle';
  }
}

/**
 * Get the color hex code for a check status
 * @param {string} status - The check status (PASSED, WARNING, FAILED, etc.)
 * @returns {string} Color hex code
 */
export function getStatusColor(status) {
  switch (status) {
    case CheckStatus.PASSED:
      return '#198754'; // Bootstrap success
    case CheckStatus.WARNING:
      return '#ffc107'; // Bootstrap warning
    case CheckStatus.FAILED:
      return '#dc3545'; // Bootstrap danger
    case CheckStatus.NO_DATA:
    case CheckStatus.UNKNOWN:
    default:
      return '#6c757d'; // Bootstrap secondary
  }
}

/**
 * Get the background color hex code for a check status
 * @param {string} status - The check status (PASSED, WARNING, FAILED, etc.)
 * @returns {string} Background color hex code
 */
export function getStatusBgColor(status) {
  switch (status) {
    case CheckStatus.PASSED:
      return '#d1e7dd'; // Bootstrap success background
    case CheckStatus.WARNING:
      return '#fff3cd'; // Bootstrap warning background
    case CheckStatus.FAILED:
      return '#f8d7da'; // Bootstrap danger background
    case CheckStatus.NO_DATA:
    case CheckStatus.UNKNOWN:
    default:
      return '#e2e3e5'; // Bootstrap secondary background
  }
}

/**
 * Get the Bootstrap text color class for a check status
 * @param {string} status - The check status (PASSED, WARNING, FAILED, etc.)
 * @returns {string} Bootstrap text color class
 */
export function getStatusTextClass(status) {
  switch (status) {
    case CheckStatus.PASSED:
      return 'text-success';
    case CheckStatus.WARNING:
      return 'text-warning';
    case CheckStatus.FAILED:
      return 'text-danger';
    case CheckStatus.NO_DATA:
    case CheckStatus.UNKNOWN:
    default:
      return 'text-secondary';
  }
}

/**
 * Get icon with text color class for a check status
 * @param {string} status - The check status (PASSED, WARNING, FAILED, etc.)
 * @param {boolean} filled - Whether to use filled icon variant (default: true)
 * @returns {string} Icon class with color class
 */
export function getStatusIconWithColor(status, filled = true) {
  const icon = getStatusIcon(status, filled);
  const colorClass = getStatusTextClass(status);
  return `${icon} ${colorClass}`;
}
