/**
 * Utility functions for server status handling
 */

/**
 * Get CSS class for server status badge
 * @param {string} status - Server status
 * @returns {string} CSS class name
 */
export function getStatusClass(status) {
  const classes = {
    ACTIVE: 'status-active',
    INACTIVE: 'status-inactive',
    ERROR: 'status-error',
    PENDING: 'status-pending',
  };
  return classes[status] || 'status-unknown';
}

/**
 * Get Bootstrap icon class for server status
 * @param {string} status - Server status
 * @returns {string} Bootstrap icon class
 */
export function getStatusIcon(status) {
  const icons = {
    ACTIVE: 'bi bi-check-circle-fill',
    INACTIVE: 'bi bi-pause-circle-fill',
    ERROR: 'bi bi-x-circle-fill',
    PENDING: 'bi bi-clock-fill',
  };
  return icons[status] || 'bi bi-question-circle-fill';
}

/**
 * Get Bootstrap badge class for server status (used in detail page)
 * @param {string} status - Server status
 * @returns {string} Bootstrap badge class
 */
export function getStatusBadgeClass(status) {
  const classes = {
    ACTIVE: 'bg-success',
    INACTIVE: 'bg-warning',
    ERROR: 'bg-danger',
    PENDING: 'bg-warning',
  };
  return classes[status] || 'bg-secondary';
}

/**
 * Format status text for display
 * @param {string} status - Server status
 * @returns {string} Formatted status text
 */
export function formatStatus(status) {
  if (!status) return 'Unknown';
  return status.charAt(0) + status.slice(1).toLowerCase();
}

/**
 * Get tooltip text explaining the status
 * @param {string} status - Server status
 * @returns {string} Tooltip explanation text
 */
export function getStatusTooltip(status) {
  const tooltips = {
    ACTIVE: 'Server connection is active. Reports are being sent to this central server.',
    INACTIVE:
      'Server connection is inactive. Reports will not be sent until the connection is reactivated.',
    ERROR:
      'Connection error detected. Please check server configuration or contact your administrator.',
    PENDING:
      'Registration submitted successfully. Waiting for administrator approval on the central server before reports can be sent.',
  };
  return tooltips[status] || 'Server status is unknown. Please refresh or contact support.';
}
