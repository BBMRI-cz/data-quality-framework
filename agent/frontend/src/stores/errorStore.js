import { ref } from 'vue';

const showErrorPage = ref(false);
const errorCode = ref(null);

export function useErrorStore() {
  function showError(code) {
    errorCode.value = code;
    showErrorPage.value = true;
  }

  function hideError() {
    showErrorPage.value = false;
    errorCode.value = null;
  }

  return {
    showErrorPage,
    errorCode,
    showError,
    hideError,
  };
}

