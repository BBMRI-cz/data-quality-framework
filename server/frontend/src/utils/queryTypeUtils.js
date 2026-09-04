/**
 * Query types supported by quality check versions, as reported by the backend.
 */
export const QUERY_TYPES = ['SQL', 'CQL', 'PYTHON', 'UNKNOWN'];

const QUERY_TYPE_LABELS = {
  SQL: 'SQL',
  CQL: 'CQL',
  PYTHON: 'Python',
  UNKNOWN: 'Unknown',
};

/**
 * Format a backend query type for display.
 *
 * @param {string} type - Query type reported by the backend (e.g. "SQL", "PYTHON")
 * @returns {string} human-readable label
 */
export function formatQueryType(type) {
  return QUERY_TYPE_LABELS[type] || QUERY_TYPE_LABELS.UNKNOWN;
}
