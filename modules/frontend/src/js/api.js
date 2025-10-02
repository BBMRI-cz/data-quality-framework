import axios from 'axios';

const state = {
    authHeader: null,
};

export function clearAuth() {
    state.authHeader = null;
    sessionStorage.removeItem('username');
    sessionStorage.removeItem('defaultPasswordFlag');
    sessionStorage.removeItem('userId');
}

export function isAuthenticated() {
    return !!state.authHeader;
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

export const api = axios.create({
});

api.interceptors.request.use((config) => {
    config.headers = config.headers || {};
    if (state.authHeader && !config.__skipAuth) {
        config.headers['Authorization'] = state.authHeader;
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
                window.location.assign(`/login?redirect=${redirect}`);
            }
        }
        return Promise.reject(err);
    }
);

export async function authenticate(username, password) {
    const authHeader = `Basic ${baseToken(username, password)}`;
    const res = await api.get('/api/login', {
        headers: {
            Authorization: authHeader,
            Accept: 'application/json',
        },
        validateStatus: () => true,
        __skipAuth: true,
    });

    if (res.status !== 200) {
        throw new Error('Invalid username or password');
    }

    state.authHeader = authHeader;

    const serverUsername = res?.data?.username && String(res.data.username).trim()
        ? res.data.username
        : username;

    sessionStorage.setItem('username', serverUsername);

    if (res.data && typeof res.data.defaultPassword === 'boolean') {
        sessionStorage.setItem('defaultPasswordFlag', res.data.defaultPassword.toString());
    }

    if (res.data && res.data.userId) {
        sessionStorage.setItem('userId', res.data.userId.toString());
    }

    return {
        username: serverUsername,
        defaultPassword: res.data?.defaultPassword || false,
        userId: res.data?.userId || null
    };
}

function baseToken(username, password) {
    return btoa(`${username}:${password}`);
}