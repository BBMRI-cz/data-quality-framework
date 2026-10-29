import { defineStore } from 'pinia';
import { reportService } from '@/services/reportService.js';
import { notificationService } from '@/services/notificationService.js';

export const useReportStore = defineStore('report', {
  state: () => ({
    reports: [],
    currentReport: null,
    latestReport: null,
    isLoading: false,
    isGenerating: false,
    pagination: {
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    },
  }),

  actions: {
    async fetchReports({ page = 0, size = 5 } = {}) {
      this.isLoading = true;
      try {
        const result = await reportService.getAll({ page, size });
        this.reports = result.items;
        this.pagination = {
          page: result.page.number ?? page,
          size: result.page.size ?? size,
          totalElements: result.page.totalElements ?? 0,
          totalPages: result.page.totalPages ?? 0,
        };
      } catch (err) {
        console.error(err);
        this.reports = [];
      } finally {
        this.isLoading = false;
      }
    },

    async fetchLatestReport() {
      try {
        this.latestReport = await reportService.getLatest();
        return this.latestReport;
      } catch (err) {
        console.error(err);
        this.latestReport = null;
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

        const completed = await reportService.pollUntilComplete(reportUrl);
        if (completed.status === 'FAILED') {
          notificationService.error(
            'Report Generation Failed',
            'The report could not be generated. Please check the agent logs for details.'
          );
        }
        await Promise.all([
          this.fetchLatestReport(),
          this.fetchReports({ page: this.pagination.page, size: this.pagination.size }),
        ]);
      } catch (err) {
        if (err?.response?.status === 409) {
          notificationService.warning(
            'Report Generation In Progress',
            err.response?.data?.detail ||
              'A report is already being generated. Please wait for it to finish and try again.'
          );
        } else {
          console.error(err);
          notificationService.error(
            'Report Generation Failed',
            'Unable to generate the report. Please try again.'
          );
        }
      } finally {
        this.isGenerating = false;
      }
    },
  },
});
