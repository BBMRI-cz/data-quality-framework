import { api } from '@/js/api.js';

export const qualityCheckService = {
  async get(id) {
    const response = await api.get(`/api/quality-checks/${id}`);
    return response.data;
  },

  async create(data) {
    const response = await api.post('/api/quality-checks', data);
    return response.data;
  },

  async update(id, data) {
    const response = await api.put(`/api/quality-checks/${id}`, data);
    return response.data;
  },

  async delete(id) {
    await api.delete(`/api/quality-checks/${id}`);
  }
};
