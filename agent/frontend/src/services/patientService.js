/**
 * Patient service
 * Handles patient data API calls
 */
import { api } from '@/api';

/**
 * Fetches patient data by ID
 * @param {string} patientId
 * @returns {Promise<object>}
 */
export async function get(patientId) {
  const response = await api.get(`/api/entities/Patient/${patientId}`);
  return response.data;
}

export const patientService = {
  get,
};
