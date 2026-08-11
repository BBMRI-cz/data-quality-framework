/**
 * Diagnostics service
 * Fetches health, application info, and in-memory log entries for the diagnostics page.
 */
import { api } from '@/api';

/**
 * Fetches the application health status (actuator).
 * @returns {Promise<{status: string, components?: object}>}
 */
export async function getHealth() {
  const { data } = await api.get('/api/health');
  return data;
}

/**
 * Fetches the application build/git information (actuator).
 * @returns {Promise<object>}
 */
export async function getInfo() {
  const { data } = await api.get('/api/info');
  return data;
}

/**
 * Fetches the most recent log entries captured in memory.
 * @returns {Promise<Array<{timestamp: string, level: string, loggerName: string, message: string}>>}
 */
export async function getLogs() {
  const { data } = await api.get('/api/logs');
  return data;
}

/**
 * Fetches the current configuration of a specific logger.
 * @param {string} name - Logger name (e.g. "ROOT").
 * @returns {Promise<{configuredLevel: string|null, effectiveLevel: string|null}>}
 */
export async function getLogger(name) {
  const { data } = await api.get(`/api/loggers/${name}`);
  return data;
}

/**
 * Sets the log level of a specific logger.
 * @param {string} name - Logger name (e.g. "ROOT").
 * @param {string} level - One of TRACE, DEBUG, INFO, WARN, ERROR, OFF.
 */
export async function setLogLevel(name, level) {
  await api.post(`/api/loggers/${name}`, { configuredLevel: level });
}

export const diagnosticsService = {
  getHealth,
  getInfo,
  getLogs,
  getLogger,
  setLogLevel,
};
