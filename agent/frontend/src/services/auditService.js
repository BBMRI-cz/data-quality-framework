/**
 * Audit service
 * Handles audit log API calls
 */
import { api } from '@/api';

const BASE_URL = '/api/audit-logs';

/**
 * Fetches paginated audit log entries, newest first
 * @param {object} options - Pagination and filter options
 * @param {number} options.page - Page number (0-based)
 * @param {number} options.size - Page size (default: 20)
 * @param {string} [options.action] - Exact action type, e.g. LOGIN_SUCCESS
 * @param {string} [options.search] - Free text matched against actor, action and details
 * @param {string} [options.dateFrom] - Only entries at or after this local date-time (ISO)
 * @param {string} [options.dateTo] - Only entries at or before this local date-time (ISO)
 * @returns {Promise<{items: Array, page: object}>}
 */
export async function getAll({ page = 0, size = 20, ...filters } = {}) {
  const { data } = await api.get(BASE_URL, { params: { page, size, ...toFilterParams(filters) } });
  return {
    items: data._embedded?.['audit-logs'] || [],
    page: data.page || { number: 0, size, totalElements: 0, totalPages: 0 },
  };
}

/**
 * Downloads every audit log entry matching the filters as CSV (pagination is ignored)
 * @param {object} filters - Same filters as {@link getAll}, without page and size
 * @returns {Promise<{blob: Blob, filename: string}>}
 */
export async function exportCsv(filters = {}) {
  const response = await api.get(`${BASE_URL}/export`, {
    params: toFilterParams(filters),
    responseType: 'blob',
  });
  const disposition = response.headers['content-disposition'] || '';
  const match = disposition.match(/filename="?([^";]+)"?/);
  return { blob: response.data, filename: match ? match[1] : 'audit-log.csv' };
}

/**
 * Fetches every audit action an entry can have
 * @returns {Promise<string[]>} Audit action enum values, e.g. LOGIN_SUCCESS
 */
export async function getActions() {
  const { data } = await api.get(`${BASE_URL}/actions`);
  return data;
}

/**
 * Builds the query parameters for the audit log filters, omitting empty ones
 * @param {object} filters
 * @returns {object}
 */
function toFilterParams({ action, search, dateFrom, dateTo } = {}) {
  const params = {};
  if (action) params.action = action;
  if (search) params.search = search;
  if (dateFrom) params.dateFrom = dateFrom;
  if (dateTo) params.dateTo = dateTo;
  return params;
}

export const auditService = {
  getAll,
  exportCsv,
  getActions,
};
