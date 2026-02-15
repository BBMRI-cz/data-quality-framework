/**
 * Axios API client configuration
 * Single configured instance for all API calls
 */
import axios from 'axios';
import { getAuthToken, clearAuth } from './auth.js';
import { useErrorStore } from '@/stores/errorStore.js';
import { notificationService } from '@/services/notificationService.js';

export const api = axios.create({
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - adds auth token
api.interceptors.request.use((config) => {
  config.headers = config.headers || {};

  const token = getAuthToken();
  if (token && !config.__skipAuth) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }

  return config;
});

// Response interceptor - handles errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth();
      const redirect = encodeURIComponent(window.location.pathname + window.location.search);
      if (window.location.pathname !== '/login') {
        window.location.assign(`/login?redirect=${redirect}&sessionExpired=true`);
      }
    } else if (error.response?.status === 400) {
      const message =
        error.response?.data?.detail || error.response?.data?.message || 'Bad request';
      notificationService.error('Error', message, { autoClose: false });
    } else if (error.response?.status === 404) {
      if (window.location.pathname !== '/login') {
        const errorStore = useErrorStore();
        errorStore.showError(error.response.status);
      }
    }
    return Promise.reject(error);
  }
);
