/**
 * Server service
 * Handles server management API calls
 */
import { api } from '@/api';

const BASE_URL = '/api/servers';

/**
 * Fetches all servers
 * @returns {Promise<Array>}
 */
export async function getAll() {
  const response = await api.get(BASE_URL);

  if (response.data._embedded && response.data._embedded.servers) {
    return response.data._embedded.servers.map((item) => item.content || item);
  } else if (Array.isArray(response.data)) {
    return response.data;
  }
  return [];
}

/**
 * Fetches a server by ID
 * @param {string|number} id
 * @returns {Promise<object>}
 */
export async function get(id) {
  const response = await api.get(`${BASE_URL}/${id}`);
  return response.data;
}

/**
 * Creates a new server
 * @param {{url: string, name: string}} serverData
 * @returns {Promise<object>}
 */
export async function create(serverData) {
  const response = await api.post(BASE_URL, {
    url: serverData.url,
    name: serverData.name,
  });
  return response.data.content || response.data;
}

/**
 * Updates an existing server
 * @param {string|number} id
 * @param {{url: string, name: string}} updateData
 * @returns {Promise<object>}
 */
export async function update(id, updateData) {
  const response = await api.put(`${BASE_URL}/${id}`, {
    url: updateData.url,
    name: updateData.name,
  });
  return response.data.content || response.data;
}

/**
 * Deletes a server
 * @param {string|number} id
 * @returns {Promise<void>}
 */
export async function remove(id) {
  await api.delete(`${BASE_URL}/${id}`);
}

export const serverService = {
  getAll,
  get,
  create,
  update,
  delete: remove,
};
