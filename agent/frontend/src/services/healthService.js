/**
 * Health service
 * Handles health check API calls
 */
import { api } from '@/api';

/**
 * Checks the health status of the FHIR server
 * @returns {Promise<{status: string, details?: object}>}
 */
export async function checkHealth() {
  const { data } = await api.get('/api/entities/health');
  return data;
}

export const healthService = {
  checkHealth,
};
