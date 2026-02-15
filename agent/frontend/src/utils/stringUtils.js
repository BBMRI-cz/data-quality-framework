/**
 * Truncates a string to a specified maximum length with ellipsis
 * @param {string} text - The text to truncate
 * @param {number} maxLength - Maximum length before truncation (default: 50)
 * @returns {string} Truncated string with ellipsis if needed
 */
export function truncateText(text, maxLength = 50) {
  if (!text) return '';
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

/**
 * Formats a date string to short format (e.g., "Jan 15, 2024")
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date
 */
export function formatDateShort(dateString) {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/**
 * Formats a date string to time format (e.g., "02:30 PM")
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted time
 */
export function formatTime(dateString) {
  const date = new Date(dateString);
  return date.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Checks if a string is valid JSON
 * @param {string} str - String to check
 * @returns {boolean} True if valid JSON
 */
export function isValidJson(str) {
  try {
    JSON.parse(str);
    return true;
  } catch {
    return false;
  }
}
