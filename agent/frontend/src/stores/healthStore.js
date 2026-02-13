import { defineStore } from 'pinia';
import { healthService } from '@/services/healthService.js';

export const useHealthStore = defineStore('health', {
  state: () => ({
    healthStatus: null,
    isChecking: false,
    lastChecked: null,
  }),

  actions: {
    async checkHealth() {
      this.isChecking = true;
      try {
        this.healthStatus = await healthService.checkHealth();
        this.lastChecked = new Date();
      } catch (err) {
        console.error('Health check failed:', err);
        this.healthStatus = {
          status: 'DOWN',
          details: {
            error: err.response?.data?.details?.error || err.message || 'Connection failed',
          },
        };
        this.lastChecked = new Date();
      } finally {
        this.isChecking = false;
      }
    },
  },
});
