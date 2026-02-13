import { defineStore } from 'pinia';
import { reportService } from '@/services/reportService.js';

export const useReportStore = defineStore('report', {
  state: () => ({
    reports: [],
    currentReport: null,
    isGenerating: false,
  }),

  actions: {
    async fetchReports() {
      try {
        this.reports = await reportService.getAll();
      } catch (err) {
        console.error(err);
        this.reports = [];
      }
    },

    async fetchReportById(id) {
      try {
        this.currentReport = await reportService.get(id);
        return this.currentReport;
      } catch (err) {
        console.error(err);
        this.currentReport = null;
        throw err;
      }
    },

    async generateReport() {
      this.isGenerating = true;
      try {
        const report = await reportService.generate();
        const reportUrl = report._links.self.href;

        await reportService.pollUntilComplete(reportUrl);
        await this.fetchReports();
      } catch (err) {
        console.error(err);
      } finally {
        this.isGenerating = false;
      }
    },
  },
});
