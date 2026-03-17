/**
 * Settings service
 * Handles settings API calls
 */
import { api } from '@/api';

const BASE_URL = '/api/settings';

/**
 * Fetches all application settings (FHIR + differential privacy).
 * @returns {Promise<object>}
 */
export async function get() {
  const response = await api.get(BASE_URL);
  return response.data;
}

/**
 * Updates all application settings (FHIR + differential privacy) in one call.
 * @param {object} settingsData
 * @returns {Promise<object>}
 */
export async function update(settingsData) {
  const response = await api.put(BASE_URL, settingsData);
  return response.data;
}

export const settingsService = {
  get,
  update,
};
