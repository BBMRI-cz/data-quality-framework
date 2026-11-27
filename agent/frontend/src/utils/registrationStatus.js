/**
 * Utility functions for registration lifecycle status presentation
 */

/** Registration states after which the frontend stops polling for updates. */
export const TERMINAL_REGISTRATION_STATES = ['REGISTERED_PENDING_APPROVAL', 'ACTIVE', 'FAILED'];

const LABELS = {
  UNREGISTERED: 'Unregistered',
  REGISTERING: 'Registering',
  REGISTERED_PENDING_APPROVAL: 'Registered - pending approval',
  ACTIVE: 'Active',
  FAILED: 'Failed',
};

const BADGE_CLASSES = {
  UNREGISTERED: 'bg-secondary',
  REGISTERING: 'bg-info text-dark',
  REGISTERED_PENDING_APPROVAL: 'bg-warning text-dark',
  ACTIVE: 'bg-success',
  FAILED: 'bg-danger',
};

const TOOLTIPS = {
  UNREGISTERED: 'No registration has been attempted with this central server.',
  REGISTERING: 'A registration request is in flight; this page updates automatically.',
  REGISTERED_PENDING_APPROVAL:
    'The central server accepted this agent. An administrator still needs to approve it.',
  ACTIVE: 'Registered and approved: the agent exchanges data with the central server.',
  FAILED: 'The last registration attempt failed. See the error details below.',
};

/**
 * Get user-friendly label for a registration status
 * @param {string} status - Registration status
 * @returns {string} Formatted label
 */
export function formatRegistrationStatus(status) {
  return LABELS[status] || status;
}

/**
 * Get Bootstrap badge classes for a registration status
 * @param {string} status - Registration status
 * @returns {string} Bootstrap badge classes
 */
export function getRegistrationBadgeClass(status) {
  return BADGE_CLASSES[status] || 'bg-secondary';
}

/**
 * Get a short explanatory tooltip for a registration status
 * @param {string} status - Registration status
 * @returns {string} Tooltip text
 */
export function getRegistrationTooltip(status) {
  return TOOLTIPS[status] || '';
}

/**
 * Whether a manual registration retry makes sense for this state
 * @param {string} status - Registration status
 * @returns {boolean}
 */
export function canRetryRegistration(status) {
  return status === 'FAILED' || status === 'UNREGISTERED';
}
