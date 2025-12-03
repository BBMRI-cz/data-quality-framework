<template>
  <div class="servers-page">
    <PageHeader
      title="Reporting Management"
      mobileTitle="Servers"
      subtitle="Manage and register central servers to which reports should be sent"
      icon="bi bi-server"
    />

    <div class="page-content">
      <!-- Loading State -->
      <div v-if="serverStore.loading && servers.length === 0" class="loading-card">
        <i class="bi bi-arrow-clockwise spinning"></i>
        <p>Loading servers...</p>
      </div>

      <!-- No Servers - Show Registration Form -->
      <ServerRegistrationForm
        v-else-if="servers.length === 0"
        :loading="isRegistering"
        @submit="registerServer"
        ref="registrationForm"
      />

      <!-- Has Servers - Show First Server Details -->
      <div v-else class="servers-container">
        <div class="section-header">
          <div>
            <h2 class="section-title">
              <i class="bi bi-server"></i>
              Registered Server
            </h2>
            <p class="section-description">Your central server configuration</p>
          </div>
          <button class="btn btn-refresh" @click="refreshServers" :disabled="serverStore.loading">
            <i class="bi bi-arrow-clockwise" :class="{ spinning: serverStore.loading }"></i>
            Refresh
          </button>
        </div>

        <ServerDetailsCard
          :server="firstServer"
          @delete="handleDelete"
          @viewDetails="handleViewDetails"
        />

        <div v-if="servers.length > 1" class="additional-servers-notice">
          <i class="bi bi-info-circle"></i>
          <span>{{ servers.length - 1 }} additional server{{ servers.length > 2 ? 's' : '' }} registered</span>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <DeleteConfirmModal
      v-if="showDeleteModal && deletingServer"
      :item-name="deletingServer.name"
      :loading="isDeleting"
      @close="closeDeleteModal"
      @confirm="deleteServer"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import serverStore from '../stores/serverStore.js';
import PageHeader from '../components/PageHeader.vue';
import ServerRegistrationForm from '../components/ServerRegistrationForm.vue';
import ServerDetailsCard from '../components/ServerDetailsCard.vue';
import DeleteConfirmModal from '../components/DeleteConfirmModal.vue';
import { notificationService } from '../services/notificationService.js';

const router = useRouter();

const isRegistering = ref(false);
const isDeleting = ref(false);
const showDeleteModal = ref(false);
const registrationForm = ref(null);

const deletingServer = ref(null);

const servers = computed(() => serverStore.servers);
const firstServer = computed(() => servers.value[0] || null);

async function registerServer(data) {
  if (!data.name || !data.url) {
    notificationService.error('Validation Error', 'Please fill in all required fields');
    return;
  }

  isRegistering.value = true;
  try {
    await serverStore.createServer(data);
    notificationService.success('Server Registered', `${data.name} has been registered successfully`);
    registrationForm.value?.clearForm();
  } catch (error) {
    notificationService.error('Registration Failed', serverStore.error || 'Unable to register server. Please try again.');
  } finally {
    isRegistering.value = false;
  }
}

function handleViewDetails(server) {
  router.push(`/servers/${server.id}`);
}

async function deleteServer() {
  if (!deletingServer.value) return;

  isDeleting.value = true;
  try {
    await serverStore.deleteServer(deletingServer.value.id);
    notificationService.success('Server Deleted', `${deletingServer.value.name} has been deleted successfully`);
    closeDeleteModal();
  } catch (error) {
    notificationService.error('Delete Failed', serverStore.error || 'Unable to delete server. Please try again.');
  } finally {
    isDeleting.value = false;
  }
}

function handleDelete(server) {
  deletingServer.value = server;
  showDeleteModal.value = true;
}

function closeDeleteModal() {
  showDeleteModal.value = false;
  deletingServer.value = null;
}

async function refreshServers() {
  try {
    await serverStore.fetchServers();
  } catch (error) {
    notificationService.error('Refresh Failed', 'Unable to refresh servers list. Please try again.');
  }
}

onMounted(() => {
  refreshServers();
});
</script>

<style scoped>
.servers-page {
  min-height: 100%;
  padding: var(--spacing-xl);
}

.page-content {
  max-width: 900px;
  margin: 0 auto;
}

/* Loading State */
.loading-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  text-align: center;
  padding: 4rem var(--spacing-xl);
  color: var(--color-gray-500);
}

.loading-card i {
  font-size: 3rem;
  color: var(--color-primary);
  margin-bottom: var(--spacing-lg);
  display: block;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Server Container */
.servers-container {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-2xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: var(--spacing-lg);
  border-bottom: 2px solid var(--color-gray-100);
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-gray-800);
  margin: 0 0 var(--spacing-sm) 0;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.section-title i {
  color: var(--color-primary);
  font-size: 1.75rem;
}

.section-description {
  font-size: 0.95rem;
  color: var(--color-gray-500);
  margin: 0;
}

.btn {
  padding: 0.875rem var(--spacing-xl);
  font-size: 1rem;
  font-weight: 600;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-refresh {
  background: var(--color-gray-100);
  color: var(--color-gray-700);
}

.btn-refresh:hover:not(:disabled) {
  background: var(--color-gray-200);
  transform: translateY(-1px);
}

/* Additional Servers Notice */
.additional-servers-notice {
  padding: var(--spacing-md) var(--spacing-lg);
  background: #eff6ff;
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  color: #1e40af;
  font-weight: 500;
}

.additional-servers-notice i {
  font-size: 1.25rem;
  color: var(--color-primary);
}

/* Responsive Design */
@media (max-width: 768px) {
  .servers-page {
    padding: var(--spacing-md);
  }

  .servers-container {
    padding: var(--spacing-lg);
  }

  .section-header {
    flex-direction: column;
    gap: var(--spacing-md);
  }
}
</style>
