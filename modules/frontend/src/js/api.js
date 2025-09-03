import axios from 'axios';

const state = {
    authHeader: null,
};

export function clearAuth() {
    state.authHeader = null;
    sessionStorage.removeItem('username');
}

export function isAuthenticated() {
    return !!state.authHeader;
}

export function getUsername() {
    return sessionStorage.getItem('username');
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
    const res = await api.get('/api/auth/login', {
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
}

function baseToken(username, password) {
    return btoa(`${username}:${password}`);
}