import axios from 'axios';
import router from '../router';
import { authStore } from '../stores/authStore';
import { notificationService } from './notificationService';

const api = axios.create({
  baseURL: '/api',
});

let isHandling401 = false;

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response && error.response.status === 401) {
      if (isHandling401) {
        return Promise.reject(error);
      }

      isHandling401 = true;

      try {
        if (authStore.isAuthenticated) {
          const currentPath = router.currentRoute.value.fullPath;
          if (currentPath && currentPath !== '/login') {
            authStore.setRedirectPath(currentPath);
          }

          authStore.logout();

          notificationService.warning(
            'Session Expired',
            'Your session has expired. Please log in again.',
            { duration: 5000, autoClose: true }
          );

          await router.push({ name: 'Login', query: { sessionExpired: 'true' } }).finally(() => {
            isHandling401 = false;
          });
        } else {
          isHandling401 = false;
        }
      } catch (err) {
        isHandling401 = false;
      }

      return Promise.reject(error);
    }

    if (error.response) {
      const status = error.response.status;
      let title = 'Error';
      let message = 'An unexpected error occurred. Please try again later.';
      switch (status) {
        case 400:
          title = 'Bad Request';
          message = error.response.data?.message || 'The request was invalid.';
          break;
        case 403:
          title = 'Forbidden';
          message = 'You do not have permission to perform this action.';
          break;
        case 404:
          title = 'Not Found';
          message = 'The requested resource was not found.';
          break;
        case 500:
          title = 'Server Error';
          message = 'A server error occurred. Please try again later.';
          break;
        default:
          if (error.response.data?.message) {
            message = error.response.data.message;
          }
          break;
      }
      notificationService.error(title, message, { duration: 5000, autoClose: true });
    } else if (error.request) {
      notificationService.error('Network Error', 'Unable to connect to the server. Please check your internet connection.', { duration: 5000, autoClose: true });
    }
    return Promise.reject(error);
  }
);

export default api;

