import { reactive } from 'vue';
import { apiService } from '@/services/apiService';
import { resetOidc } from '@/utils/oidc';

const PROFILE_MAX_AGE_MS = 60 * 1000;

export const authStore = reactive({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  redirectPath: null,
  mode: null,
  profileRefreshedAt: 0,

  get isAdmin() {
    return this.user?.roles?.includes('ADMIN') ?? false;
  },

  init() {
    const savedUser = localStorage.getItem('user');
    const savedToken = localStorage.getItem('authToken');
    const savedMode = localStorage.getItem('authMode');

    if (savedUser && savedToken) {
      this.user = JSON.parse(savedUser);
      this.isAuthenticated = true;
      this.mode = savedMode || 'basic';
    }
  },

  async setUser(user, token, mode = 'basic') {
    this.user = user;
    this.isAuthenticated = true;
    this.error = null;
    this.mode = mode;

    localStorage.setItem('authMode', mode);
    if (token) {
      localStorage.setItem('authToken', token);
    }

    if (mode === 'oidc') {
      await this.mergeUserData();
    } else {
      localStorage.setItem('user', JSON.stringify(user));
    }
  },

  logout() {
    this.user = null;
    this.isAuthenticated = false;
    this.error = null;
    this.redirectPath = null;
    this.mode = null;
    this.profileRefreshedAt = 0;

    localStorage.removeItem('user');
    localStorage.removeItem('authToken');
    localStorage.removeItem('authMode');
    localStorage.removeItem('rememberedUsername');

    resetOidc();
  },

  setLoading(loading) {
    this.isLoading = loading;
  },

  setError(error) {
    this.error = error;
  },

  clearError() {
    this.error = null;
  },

  setRedirectPath(path) {
    this.redirectPath = path;
  },

  getRedirectPath() {
    const path = this.redirectPath;
    this.redirectPath = null; // Clear after getting
    return path || '/dashboard';
  },

  async mergeUserData() {
    if (!this.user) return;

    this.setLoading(true);
    try {
      const userData = await apiService.getUserProfile();
      this.user = {
        ...this.user,
        id: userData.id,
        agentId: userData.agentId,
        roles: userData.roles,
        username: userData.username,
      };
      this.profileRefreshedAt = Date.now();
    } catch (error) {
      console.error('Failed to fetch user profile:', error);
      this.setError('Failed to load user profile data');
    } finally {
      localStorage.setItem('user', JSON.stringify(this.user));
      this.setLoading(false);
    }
  },

  /**
   * Re-reads the current user's profile so role changes made by an administrator take effect
   * during an active session (both OIDC and internal-token sessions). Failures keep the cached
   * profile.
   */
  async refreshProfile() {
    if (!this.isAuthenticated || !this.user) return;

    try {
      const userData = await apiService.getUserProfile();
      if (!this.isAuthenticated) return;
      this.user = {
        ...this.user,
        id: userData.id,
        agentId: userData.agentId,
        roles: userData.roles,
        username: userData.username,
      };
      this.profileRefreshedAt = Date.now();
      localStorage.setItem('user', JSON.stringify(this.user));
    } catch (error) {
      console.error('Failed to refresh user profile:', error);
    }
  },

  async refreshProfileIfStale(maxAgeMs = PROFILE_MAX_AGE_MS) {
    if (Date.now() - this.profileRefreshedAt >= maxAgeMs) {
      await this.refreshProfile();
    }
  },
});

authStore.init();
