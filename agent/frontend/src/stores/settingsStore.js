import { defineStore } from 'pinia';
import { settingsService } from '@/services/settingsService.js';

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    settings: null,
    loading: false,
    error: null,
  }),

  actions: {
    async fetchSettings() {
      this.loading = true;
      this.error = null;
      try {
        this.settings = await settingsService.get();
        return this.settings;
      } catch (err) {
        console.error('Failed to fetch settings', err);
        this.error = err.response?.data?.message || 'Failed to fetch settings';
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async updateSettings(settingsData) {
      this.loading = true;
      this.error = null;
      try {
        this.settings = await settingsService.update(settingsData);
        return this.settings;
      } catch (err) {
        console.error('Failed to update settings', err);
        this.error = err.response?.data?.message || 'Failed to update settings';
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async fetchDiffPrivacySettings() {
      this.loading = true;
      this.error = null;
      try {
        const privacySettings = await settingsService.getDiffPrivacySettings();
        return privacySettings;
      } catch (err) {
        console.error('Failed to fetch differential privacy settings', err);
        this.error = err.response?.data?.message || 'Failed to fetch privacy settings';
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async updateDiffPrivacySettings(privacyData) {
      this.loading = true;
      this.error = null;
      try {
        const updatedSettings = await settingsService.updateDiffPrivacySettings(privacyData);
        return updatedSettings;
      } catch (err) {
        console.error('Failed to update differential privacy settings', err);
        this.error = err.response?.data?.message || 'Failed to update privacy settings';
        throw err;
      } finally {
        this.loading = false;
      }
    },
  },
});
