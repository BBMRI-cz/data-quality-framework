import axios from 'axios';
import { useErrorStore } from '../stores/errorStore.js';

const state = {
    authToken: null,
};

const errorStore = useErrorStore();

export function clearAuth() {
    state.authToken = null;
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('username');
    sessionStorage.removeItem('defaultPasswordFlag');
    sessionStorage.removeItem('userId');
}

export function isAuthenticated() {
    return !!state.authToken;
}

export function getUsername() {
    return sessionStorage.getItem('username');
}

export function getUserId() {
    return sessionStorage.getItem('userId');
}

export function getDefaultPasswordFlag() {
    const stored = sessionStorage.getItem('defaultPasswordFlag');
    return stored === 'true';
}

export function getAuthToken() {
    return state.authToken || sessionStorage.getItem('authToken');
}

// Initialize token from sessionStorage on page load
function initializeAuth() {
    const storedToken = sessionStorage.getItem('authToken');
    if (storedToken) {
        state.authToken = storedToken;
    }
}

initializeAuth();

export const api = axios.create({
});

api.interceptors.request.use((config) => {
    config.headers = config.headers || {};
    const token = getAuthToken();
    if (token && !config.__skipAuth) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (r) => r,
    (err) => {
        if (err.response?.status === 401) {
            clearAuth();
            const redirect = encodeURIComponent(window.location.pathname + window.location.search);
            if (window.location.pathname !== '/login') {
                window.location.assign(`/login?redirect=${redirect}&sessionExpired=true`);
            }
        } else if (err.response?.status === 404) {
            if (window.location.pathname !== '/login') {
                errorStore.showError(err.response.status);
            }
        }
        return Promise.reject(err);
    }
);

export async function authenticate(username, password) {
    try {
        const res = await api.post('/api/auth/login', {
            username,
            password
        }, {
            headers: {
                'Content-Type': 'application/json',
            },
            validateStatus: () => true,
            __skipAuth: true,
        });

        if (res.status !== 200) {
            throw new Error('Invalid username or password');
        }

        const { token, user } = res.data;

        if (!token) {
            throw new Error('No token received from server');
        }

        state.authToken = token;
        sessionStorage.setItem('authToken', token);

        const serverUsername = user?.username && String(user.username).trim()
            ? user.username
            : username;

        sessionStorage.setItem('username', serverUsername);

        if (user && typeof user.defaultPassword === 'boolean') {
            sessionStorage.setItem('defaultPasswordFlag', user.defaultPassword.toString());
        }

        if (user && user.userId) {
            sessionStorage.setItem('userId', user.userId.toString());
        }

        return {
            username: serverUsername,
            defaultPassword: user?.defaultPassword || false,
            userId: user?.userId || null
        };
    } catch (error) {
        if (error.response?.status === 401) {
            throw new Error('Invalid username or password');
        }
        throw error;
    }
}

/**
 * Validates a server URL by checking if it has a valid /api/info endpoint
 * @param {string} serverUrl - The base URL of the server to validate
 * @returns {Promise<{valid: boolean, version?: string, error?: string}>}
 */
export async function validateServerUrl(serverUrl) {
    try {
        // Remove trailing slash if present
        const baseUrl = serverUrl.replace(/\/$/, '');

        const response = await axios.get(`${baseUrl}/api/info`, {
            timeout: 5000,
            validateStatus: (status) => status === 200
        });

        if (response.status === 200 && response.data?.build?.version) {
            return {
                valid: true,
                version: response.data.build.version
            };
        }

        return {
            valid: false,
            error: 'Invalid response format'
        };
    } catch (error) {
        return {
            valid: false,
            error: error.response?.status === 404
                ? 'Server found but /api/info endpoint not available'
                : error.code === 'ECONNABORTED'
                ? 'Connection timeout - is the URL correct?'
                : 'Unable to connect - is the URL correct?'
        };
    }
}

/**
 * Fetches application info including version
 * @returns {Promise<{version: string}>}
 */
export async function getAppInfo() {
    try {
        const response = await api.get('/api/info', {
            __skipAuth: true
        });

        return {
            version: response.data?.build?.version || 'unknown'
        };
    } catch (error) {
        return {
            version: 'unknown'
        };
    }
}

