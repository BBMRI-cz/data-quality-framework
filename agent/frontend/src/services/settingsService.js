/**
 * Settings service
 * Handles settings API calls
 */
import { api } from '@/api';

const BASE_URL = '/api/settings';

/**
 * Fetches application settings
 * @returns {Promise<object>}
 */
export async function get() {
  const response = await api.get(BASE_URL);
  return response.data;
}

/**
 * Updates application settings
 * @param {object} settingsData
 * @returns {Promise<object>}
 */
export async function update(settingsData) {
  const response = await api.put(BASE_URL, settingsData);
  return response.data;
}

/**
 * Fetches differential privacy settings
 * @returns {Promise<object>}
 */
export async function getDiffPrivacySettings() {
  const response = await api.get(`${BASE_URL}/differential-privacy`);
  return response.data;
}

/**
 * Updates differential privacy settings
 * @param {object} privacyData
 * @returns {Promise<object>}
 */
export async function updateDiffPrivacySettings(privacyData) {
  const response = await api.put(`${BASE_URL}/differential-privacy`, privacyData);
  return response.data;
}

export const settingsService = {
  get,
  update,
  getDiffPrivacySettings,
  updateDiffPrivacySettings,
};
