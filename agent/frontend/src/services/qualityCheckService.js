import { api } from '@/api';

const BASE_URL = '/api/quality-checks';

export const qualityCheckService = {
  /**
   * Fetches paginated quality checks
   * @param {object} options - Pagination options
   * @param {number} options.page - Page number (0-based)
   * @param {number} options.size - Page size (default: 10)
   * @returns {Promise<{items: Array, page: object}>}
   */
  async getAll({ page = 0, size = 10, categoryName = null } = {}) {
    const params = { page, size };
    if (categoryName !== null && categoryName !== undefined) {
      params.categoryName = categoryName;
    }
    const response = await api.get(BASE_URL, { params });
    return {
      items: response.data._embedded?.['quality-checks'] || [],
      page: response.data.page || { number: 0, size, totalElements: 0, totalPages: 0 },
    };
  },

  async get(id) {
    const response = await api.get(`${BASE_URL}/${id}`);
    return response.data;
  },

  async create(data) {
    const response = await api.post(BASE_URL, data);
    return response.data;
  },

  async update(id, data) {
    const response = await api.put(`${BASE_URL}/${id}`, data);
    return response.data;
  },

  /**
   * Partially updates multiple quality checks in a single request
   * @param {Array<{id: number}>} updates - Update items carrying the check ID
   * @returns {Promise<Array>} The updated quality checks
   */
  async bulkUpdate(updates) {
    const response = await api.patch(BASE_URL, updates);
    return response.data;
  },

  /**
   * Downloads the quality checks matching the filter as a portable JSON file
   * @param {object} [options] - Export options
   * @param {string} [options.categoryName] - Category filter; omit for all checks, '' for uncategorized
   * @returns {Promise<{blob: Blob, filename: string}>}
   */
  async exportAll({ categoryName = null } = {}) {
    const params = {};
    if (categoryName !== null && categoryName !== undefined) {
      params.categoryName = categoryName;
    }
    const response = await api.get(`${BASE_URL}/export`, { params, responseType: 'blob' });
    const disposition = response.headers['content-disposition'] || '';
    const match = disposition.match(/filename="?([^";]+)"?/);
    return { blob: response.data, filename: match ? match[1] : 'quality-checks.json' };
  },

  /**
   * Imports quality checks from a parsed JSON file (the export format). Errors are reported by the
   * calling view, so the API interceptor does not toast them.
   * @param {Array<object>} entries - Import entries in the export file format
   * @returns {Promise<{created: number, updated: number, unchanged: number}>} Outcome counts
   */
  async importChecks(entries) {
    const response = await api.post(`${BASE_URL}/import`, entries, {
      __skipErrorNotification: true,
    });
    return response.data;
  },

  async delete(id) {
    await api.delete(`${BASE_URL}/${id}`);
  },
};
