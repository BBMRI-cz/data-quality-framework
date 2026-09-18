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

      <ServerRegistrationForm ref="registrationForm" :loading="isRegistering" @submit="register" />
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

  async function register(data) {
    if (!data.name || !data.url) {
      notificationService.error('Validation Error', 'Please fill in all required fields');
      return;
    }

    isRegistering.value = true;
    try {
      await serverStore.createServer(data);
      notificationService.success(
        'Server Registered',
        `${data.name} has been registered successfully`
      );
      router.push('/servers');
    } catch {
      notificationService.error(
        'Registration Failed',
        serverStore.error || 'Unable to register server. Please try again.'
      );
      registrationForm.value?.clearForm();
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
