import { api } from '@/api';

const BASE_URL = '/api/categories';

function extractCategories(response) {
  const data = response.data;
  if (data._embedded && Array.isArray(data._embedded.categories)) {
    return data._embedded.categories;
  }
  if (Array.isArray(data)) {
    return data;
  }
  return [];
}

export const categoryService = {
  /**
   * Fetches all categories.
   * @returns {Promise<Array>}
   */
  async getAll() {
    const response = await api.get(BASE_URL);
    return extractCategories(response);
  },

  /**
   * Fetches a single category by ID.
   * @param {number|string} id
   * @returns {Promise<object>}
   */
  async get(id) {
    const response = await api.get(`${BASE_URL}/${id}`);
    return response.data;
  },

  /**
   * Creates a new category.
   * @param {object} data
   * @returns {Promise<object>}
   */
  async create(data) {
    const response = await api.post(BASE_URL, data);
    return response.data;
  },

  /**
   * Updates an existing category.
   * @param {number|string} id
   * @param {object} data
   * @returns {Promise<object>}
   */
  async update(id, data) {
    const response = await api.put(`${BASE_URL}/${id}`, data);
    return response.data;
  },

  /**
   * Deletes a category by ID.
   * @param {number|string} id
   * @returns {Promise<void>}
   */
  async delete(id) {
    await api.delete(`${BASE_URL}/${id}`);
  },
};
