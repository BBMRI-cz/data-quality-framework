/**
 * Manifest service
 * Handles browsing manifests published by registered central servers
 */
import { api } from '@/api';

const BASE_URL = '/api/servers';

/**
 * Fetches all manifests published by a central server
 * @param {string|number} serverId
 * @returns {Promise<Array>}
 */
export async function getManifests(serverId) {
  const response = await api.get(`${BASE_URL}/${serverId}/manifests`);
  return Array.isArray(response.data) ? response.data : [];
}

/**
 * Fetches a single manifest including its signed versions
 * @param {string|number} serverId
 * @param {string|number} manifestId
 * @returns {Promise<object>}
 */
export async function getManifest(serverId, manifestId) {
  const response = await api.get(`${BASE_URL}/${serverId}/manifests/${manifestId}`);
  return response.data;
}

export const manifestService = {
  getManifests,
  getManifest,
};
