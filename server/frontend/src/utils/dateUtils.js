/**
 * Date formatting utilities for the Data Quality Framework UI.
 *
 * All timestamps from the server are Java Instants serialized as UTC ISO-8601 strings.
 * Every function here uses `undefined` as the locale so the browser automatically applies
 * the user's local timezone and locale preferences.
 */

/**
 * Formats a UTC timestamp as a short date (e.g. "03/06/2026").
 * @param {string|null} dateString - UTC ISO-8601 timestamp string
 * @returns {string}
 */
export function formatDateShort(dateString) {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

/**
 * Formats a UTC timestamp as a long date with time (e.g. "March 6, 2026, 05:00 PM").
 * @param {string|null} dateString - UTC ISO-8601 timestamp string
 * @returns {string}
 */
export function formatDateLong(dateString) {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Formats a UTC timestamp as time only, hours and minutes (e.g. "05:00 PM").
 * @param {string|null} dateString - UTC ISO-8601 timestamp string
 * @returns {string}
 */
export function formatTime(dateString) {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Formats a UTC timestamp as time with seconds (e.g. "05:00:30 PM").
 * @param {string|null} dateString - UTC ISO-8601 timestamp string
 * @returns {string}
 */
export function formatTimeFull(dateString) {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}
