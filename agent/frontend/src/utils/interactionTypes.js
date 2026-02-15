/**
 * Utility functions for server interaction type handling
 */

/**
 * Get CSS class for interaction type badge
 * @param {string} type - Interaction type
 * @returns {string} CSS class name
 */
export function getInteractionTypeClass(type) {
  const classes = {
    UPDATE: 'type-update',
    COMMUNICATION: 'type-communication',
    REGISTRATION: 'type-registration',
  };
  return classes[type] || '';
}

/**
 * Get Bootstrap badge class for interaction type
 * @param {string} type - Interaction type
 * @returns {string} Bootstrap badge class
 */
export function getInteractionTypeBadge(type) {
  const classes = {
    UPDATE: 'bg-primary',
    COMMUNICATION: 'bg-success',
    REGISTRATION: 'bg-info',
  };
  return classes[type] || 'bg-secondary';
}

/**
 * Get Bootstrap icon class for interaction type
 * @param {string} type - Interaction type
 * @returns {string} Bootstrap icon class
 */
export function getInteractionTypeIcon(type) {
  const icons = {
    UPDATE: 'bi bi-pencil-square',
    COMMUNICATION: 'bi bi-arrow-left-right',
    REGISTRATION: 'bi bi-person-plus-fill',
  };
  return icons[type] || 'bi bi-circle';
}

/**
 * Get user-friendly label for interaction type
 * @param {string} type - Interaction type
 * @returns {string} Formatted label
 */
export function formatInteractionType(type) {
  const labels = {
    UPDATE: 'Update',
    COMMUNICATION: 'Communication',
    REGISTRATION: 'Registration',
  };
  return labels[type] || type;
}
