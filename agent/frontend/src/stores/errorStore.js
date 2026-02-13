import { defineStore } from 'pinia';

export const useErrorStore = defineStore('error', {
  state: () => ({
    showErrorPage: false,
    errorCode: null,
  }),

  actions: {
    showError(code) {
      this.errorCode = code;
      this.showErrorPage = true;
    },

    hideError() {
      this.showErrorPage = false;
      this.errorCode = null;
    },
  },
});
