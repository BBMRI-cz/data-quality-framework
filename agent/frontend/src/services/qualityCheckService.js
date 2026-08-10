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

  async delete(id) {
    await api.delete(`${BASE_URL}/${id}`);
  },
};
