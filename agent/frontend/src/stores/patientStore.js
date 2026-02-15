import { defineStore } from 'pinia';
import { patientService } from '@/services/patientService.js';

export const usePatientStore = defineStore('patient', {
  state: () => ({
    patientData: null,
  }),

  actions: {
    async fetchPatientData(patientId) {
      try {
        this.patientData = await patientService.get(patientId);
        return this.patientData;
      } catch (error) {
        console.error('Failed to fetch patientData', error);
        throw error;
      }
    },
  },
});
