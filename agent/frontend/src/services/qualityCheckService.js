import { api } from '@/js/api.js';

const BASE_URL = '/api/quality-checks';

export const qualityCheckService = {
  async getAll() {
    const response = await api.get(BASE_URL);
    return response.data._embedded?.['quality-checks'] || [];
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
  }
};
