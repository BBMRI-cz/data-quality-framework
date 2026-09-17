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

/**
 * Fetches the quality checks pinned by a specific manifest version
 * @param {string|number} serverId
 * @param {string|number} manifestId
 * @param {string|number} versionId manifest version ID on the central server
 * @returns {Promise<Array>}
 */
export async function getVersionQualityChecks(serverId, manifestId, versionId) {
  const response = await api.get(
    `${BASE_URL}/${serverId}/manifests/${manifestId}/versions/${versionId}/quality-checks`
  );
  return Array.isArray(response.data) ? response.data : [];
}

/**
 * Downloads a manifest version and persists it together with its quality checks locally
 * @param {string|number} serverId
 * @param {string|number} manifestId
 * @param {number} version the manifest version number
 * @returns {Promise<object>}
 */
export async function downloadManifestVersion(serverId, manifestId, version) {
  const response = await api.post(
    `${BASE_URL}/${serverId}/manifests/${manifestId}/versions/${version}/download`
  );
  return response.data;
}

export const manifestService = {
  getManifests,
  getManifest,
  getVersionQualityChecks,
  downloadManifestVersion,
};
