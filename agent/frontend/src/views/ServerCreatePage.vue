<template>
  <div class="server-create-page">
    <PageHeader
      title="Add Central Server"
      mobile-title="Add Server"
      subtitle="Register a central server to send quality reports to and pull quality checks from"
      icon="bi bi-hdd-network"
    />

    <div class="page-content">
      <div class="page-actions">
        <ActionButton
          to="/servers"
          icon="bi bi-arrow-left"
          text="Back to Central Servers"
          variant="secondary"
        />
      </div>

      <ServerRegistrationForm
        ref="registrationForm"
        :loading="isRegistering"
        :failure="registrationFailure"
        @submit="register"
      />
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { useServerStore } from '@/stores/serverStore.js';
  import PageHeader from '@/components/PageHeader.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import ServerRegistrationForm from '@/components/ServerRegistrationForm.vue';
  import { notificationService } from '@/services/notificationService.js';

  const router = useRouter();
  const serverStore = useServerStore();

  const isRegistering = ref(false);
  const registrationForm = ref(null);
  const registrationFailure = ref(null);

  const FAILURE_HINTS = {
    0: 'Could not reach the agent API. Check that the agent backend is running and reachable.',
    400: 'The server rejected the submitted data. Please check the form values.',
    409: 'A server with this URL is already configured on this agent. Edit or remove the existing server instead.',
    429: 'Too many registration attempts. Please wait a moment before retrying.',
  };

  async function register(data) {
    if (!data.name || !data.url) {
      notificationService.error('Validation Error', 'Please fill in all required fields');
      return;
    }

    isRegistering.value = true;
    registrationFailure.value = null;
    try {
      await serverStore.createServer(data);
      notificationService.success(
        'Registration Requested',
        `${data.name} was added. Registration request sent to the central server — check the server detail page for the outcome.`
      );
      router.push('/servers');
    } catch (error) {
      const status = error.response?.status ?? 0;
      const detail = serverStore.error || 'Unable to register server. Please try again.';
      const hint = FAILURE_HINTS[status];
      registrationFailure.value = { status, message: detail, hint: hint || null };
      notificationService.error('Registration Failed', hint ? `${detail} — ${hint}` : detail);
    } finally {
      isRegistering.value = false;
    }
  }
</script>

<style scoped>
  .server-create-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    max-width: 900px;
    margin: 0 auto;
  }

  .page-actions {
    display: flex;
    justify-content: flex-start;
    margin-bottom: var(--spacing-md);
  }

  @media (max-width: 768px) {
    .server-create-page {
      padding: var(--spacing-md);
    }
  }

  @media (max-width: 576px) {
    .server-create-page {
      padding: var(--spacing-sm);
    }
  }
</style>
