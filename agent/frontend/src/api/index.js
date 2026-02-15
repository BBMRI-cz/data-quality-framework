/**
 * API module barrel export
 * Re-exports all API-related functionality
 */
export { api } from './client.js';
export {
  clearAuth,
  isAuthenticated,
  getUsername,
  getUserId,
  getDefaultPasswordFlag,
  setDefaultPasswordFlag,
  getAuthToken,
  setAuthToken,
  setUserData,
} from './auth.js';
export { authenticate, validateServerUrl, getAppInfo } from '@/services/authService.js';
