/**
 * Authentication service
 * Handles login/logout API calls
 */
import { api } from '@/api/client.js';
import { setAuthToken, setUserData } from '@/api/auth.js';

/**
 * Authenticates a user with username and password
 * @param {string} username
 * @param {string} password
 * @returns {Promise<{username: string, defaultPassword: boolean, userId: string|null}>}
 */
export async function authenticate(username, password) {
  try {
    const res = await api.post(
      '/api/auth/login',
      { username, password },
      {
        validateStatus: () => true,
        __skipAuth: true,
      }
    );

    if (res.status !== 200) {
      throw new Error('Invalid username or password');
    }

    const { token, user } = res.data;

    if (!token) {
      throw new Error('No token received from server');
    }

    setAuthToken(token);
    return setUserData(user, username);
  } catch (error) {
    if (error.response?.status === 401) {
      throw new Error('Invalid username or password');
    }
    throw error;
  }
}

/**
 * Changes user password
 * @param {string} userId
 * @param {string} currentPassword
 * @param {string} newPassword
 * @param {string} confirmPassword
 * @returns {Promise<Response>}
 */
export async function changePassword(userId, currentPassword, newPassword, confirmPassword) {
  return api.put(`/api/users/${userId}/password`, {
    currentPassword,
    newPassword,
    confirmPassword,
  });
}

/**
 * Validates a server URL by checking if it has a valid /api/info endpoint
 * @param {string} serverUrl - The base URL of the server to validate
 * @returns {Promise<{valid: boolean, version?: string, error?: string}>}
 */
export async function validateServerUrl(serverUrl) {
  try {
    const baseUrl = serverUrl.replace(/\/$/, '');

    const response = await api.get(`${baseUrl}/api/info`, {
      timeout: 5000,
      validateStatus: (status) => status === 200,
      __skipAuth: true,
    });

    if (response.status === 200 && response.data?.build?.version) {
      return {
        valid: true,
        version: response.data.build.version,
      };
    }

    return {
      valid: false,
      error: 'Invalid response format',
    };
  } catch (error) {
    return {
      valid: false,
      error:
        error.response?.status === 404
          ? 'Server found but /api/info endpoint not available'
          : error.code === 'ECONNABORTED'
            ? 'Connection timeout - is the URL correct?'
            : 'Unable to connect - is the URL correct?',
    };
  }
}

/**
 * Fetches application info including version
 * @returns {Promise<{version: string, gitCommit: string, buildTime: string|null}>}
 */
export async function getAppInfo() {
  try {
    const response = await api.get('/api/info', {
      __skipAuth: true,
    });

    return {
      version: response.data?.build?.version || 'unknown',
      gitCommit: response.data?.git?.commit?.id?.abbrev || 'unknown',
      buildTime: response.data?.build?.time || null,
    };
  } catch {
    return {
      version: 'unknown',
      gitCommit: 'unknown',
      buildTime: null,
    };
  }
}

export const authService = {
  authenticate,
  changePassword,
  validateServerUrl,
  getAppInfo,
};
