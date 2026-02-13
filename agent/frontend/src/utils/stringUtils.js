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
