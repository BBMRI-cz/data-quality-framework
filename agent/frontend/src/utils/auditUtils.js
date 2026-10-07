/**
 * Formats an audit action for display (e.g. "LOGIN_SUCCESS" -> "Login success")
 * @param {string} action - Audit action enum value
 * @returns {string} Human-readable label
 */
export function formatAuditAction(action) {
  if (!action) return '';
  const words = action.toLowerCase().split('_');
  return [words[0].charAt(0).toUpperCase() + words[0].slice(1), ...words.slice(1)].join(' ');
}

/**
 * Returns the Bootstrap badge class for an audit action, based on its outcome
 * @param {string} action - Audit action enum value
 * @returns {string} Bootstrap background class
 */
export function getAuditActionBadgeClass(action) {
  if (!action) return 'bg-secondary';
  if (action.endsWith('_FAILURE') || action.endsWith('_FAILED') || action.endsWith('_DELETED')) {
    return 'bg-danger';
  }
  if (action.endsWith('_CREATED') || action.endsWith('_REGISTERED')) {
    return 'bg-success';
  }
  if (action.startsWith('LOGIN') || action === 'LOGOUT') {
    return 'bg-primary';
  }
  return 'bg-secondary';
}

/**
 * Formats a timestamp with date and seconds (e.g. "Oct 6, 2026, 01:21:55 PM")
 * @param {string} dateString - ISO date-time string
 * @returns {string} Formatted date-time
 */
export function formatAuditTimestamp(dateString) {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}
