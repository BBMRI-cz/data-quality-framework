import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authenticate } from '@/api';
import { useUserStore } from '@/stores/userStore.js';

/**
 * Composable for handling login form state and authentication logic
 */
export function useLoginForm() {
  const username = ref('');
  const password = ref('');
  const loading = ref(false);
  const error = ref('');
  const showPassword = ref(false);

  const route = useRoute();
  const router = useRouter();
  const userStore = useUserStore();

  function togglePasswordVisibility() {
    showPassword.value = !showPassword.value;
  }

  function clearError() {
    error.value = '';
  }

  async function login() {
    clearError();
    loading.value = true;

    try {
      const loginResult = await authenticate(username.value, password.value);
      userStore.updateDefaultPasswordStatus(loginResult.defaultPassword);

      const redirectPath = route.query.redirect ? String(route.query.redirect) : '/';
      await router.replace(redirectPath);
    } catch (e) {
      error.value = e?.message || 'Invalid username or password';
    } finally {
      loading.value = false;
    }
  }

  return {
    username,
    password,
    loading,
    error,
    showPassword,
    togglePasswordVisibility,
    login,
  };
}
