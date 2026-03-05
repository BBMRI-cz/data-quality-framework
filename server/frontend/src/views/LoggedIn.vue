<template>
  <div class="d-flex justify-content-center align-items-center" style="min-height: 100vh">
    <div class="text-center">
      <div class="spinner-border text-primary mb-3" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p>Completing login...</p>
    </div>
  </div>
</template>

<script setup>
  import { onMounted } from 'vue';
  import { useAuth, useOidcStore } from 'vue3-oidc';
  import { initializeOidc } from '../utils/oidc.js';
  import { authStore } from '../stores/authStore.js';
  import { useRouter } from 'vue-router';

  const router = useRouter();

  onMounted(async () => {
    try {
      await initializeOidc();

      const { autoAuthenticate } = useAuth();
      await autoAuthenticate();

      const oidcStore = useOidcStore();
      const user = oidcStore.state.value?.user;

      if (!user) {
        console.error('OIDC authentication failed: No user found');
        router.push('/login');
        return;
      }

      const accessToken =
        user?.access_token || oidcStore.state.value?.token?.value || oidcStore.state.value?.token;

      if (user && accessToken) {
        await authStore.setUser(user, accessToken, 'oidc');

        const redirectPath = authStore.getRedirectPath();
        router.push(redirectPath);
      } else {
        console.error('OIDC authentication failed: No access token found');
        router.push('/login');
      }
    } catch (error) {
      console.error('OIDC callback error:', error);
      router.push('/login');
    }
  });
</script>
