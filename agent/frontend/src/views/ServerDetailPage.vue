<template>
  <div class="server-detail-page">
    <PageHeader
      :title="server?.name || 'Central Server Details'"
      :mobile-title="server?.name || 'Server'"
      subtitle="Central server configuration and interaction history"
      icon="bi bi-hdd-network"
    />

    <div class="page-content">
      <!-- Back Button -->
      <div class="page-actions">
        <ActionButton
          to="/servers"
          icon="bi bi-arrow-left"
          text="Back to Central Servers"
          variant="secondary"
        />
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="alert alert-info mb-4">
        <i class="bi bi-arrow-clockwise spinning me-2"></i>
        Loading server details...
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="alert alert-danger mb-4">
        <i class="bi bi-exclamation-triangle me-2"></i>
        {{ error }}
      </div>

      <!-- Server details -->
      <template v-else-if="server">
        <!-- Stats Cards -->
        <div class="stats-grid mb-4">
          <StatCard :number="server.name" label="Server Name" number-class="text-dark" />
          <StatCard
            :number="formatStatus(server.status)"
            label="Status"
            :number-class="getStatusTextClass(server.status)"
          />
          <StatCard
            :number="server.interactions?.length || 0"
            label="Total Interactions"
            number-class="text-primary"
          />
          <StatCard :number="server.clientId || 'N/A'" label="Client ID" number-class="text-dark" />
        </div>

        <!-- Public Key Card -->
        <div class="card public-key-card mb-4 border-0 shadow-sm">
          <div class="card-header bg-white d-flex justify-content-between align-items-center">
            <div>
              <h5 class="mb-0">
                <i class="bi bi-key me-2"></i>
                Public Key
              </h5>
              <small class="text-muted">Used to verify manifests signed by this server</small>
            </div>
            <ActionButton
              :icon="server.publicKey ? 'bi bi-pencil' : 'bi bi-plus'"
              :text="server.publicKey ? 'Update Key' : 'Add Key'"
              @click="openKeyModal"
            />
          </div>
          <div class="card-body">
            <template v-if="server.publicKey">
              <span class="badge bg-success mb-2">
                <i class="bi bi-check-circle-fill me-1"></i>
                Configured
              </span>
              <pre class="key-preview">{{ server.publicKey }}</pre>
            </template>
            <span v-else class="text-muted">
              <i class="bi bi-info-circle me-2"></i>
              No public key configured. Add one to verify manifests pulled from this server.
            </span>
          </div>
        </div>

        <!-- Filters -->
        <div class="filters-section mb-4">
          <div class="filter-item">
            <label class="filter-label">Filter by Type</label>
            <select v-model="filterType" class="form-select form-select-sm">
              <option value="">All Types</option>
              <option value="UPDATE">Update</option>
              <option value="COMMUNICATION">Communication</option>
              <option value="REGISTRATION">Registration</option>
            </select>
          </div>
          <div class="filter-item">
            <label class="filter-label">Search</label>
            <input
              v-model="searchQuery"
              type="text"
              class="form-control form-control-sm"
              placeholder="Search descriptions..."
            />
          </div>
          <div class="filter-item filter-button-group">
            <button
              class="btn btn-sm btn-outline-secondary"
              :disabled="!filterType && !searchQuery"
              @click="clearFilters"
            >
              <i class="bi bi-x-circle"></i>
              Clear
            </button>
          </div>
        </div>

        <!-- Interactions Table -->
        <BaseTable
          title="Interaction History"
          :columns="interactionColumns"
          :items="paginatedInteractions"
          :total-elements="filteredInteractions.length"
          :total-pages="totalPages"
          :current-page="currentPage"
          :loading="false"
          item-key="id"
          item-label="interactions"
          empty-text="No interactions logged yet"
          empty-icon="bi bi-inbox"
          @page-change="onPageChanged"
        >
          <template #timestamp="{ item }">
            <div class="d-flex flex-column gap-1">
              <span class="fw-medium">{{ formatDateShort(item.timestamp) }}</span>
              <span class="text-muted small">{{ formatTime(item.timestamp) }}</span>
            </div>
          </template>
          <template #type="{ item }">
            <span
              :class="[
                'badge',
                getInteractionTypeBadge(item.type),
                getInteractionBadgeTextColor(item.type),
              ]"
            >
              {{ item.type }}
            </span>
          </template>
          <template #description="{ item }">
            <div class="d-flex align-items-center gap-2">
              <span>{{ truncateText(item.description, 50) }}</span>
              <button
                v-if="isValidJson(item.description)"
                class="btn btn-sm btn-outline-primary"
                title="View JSON details"
                @click.stop="openJsonModal(item.description)"
              >
                <i class="bi bi-braces"></i>
                JSON
              </button>
            </div>
          </template>
        </BaseTable>
      </template>
    </div>

    <!-- JSON Viewer Modal -->
    <BaseModal
      :show="showJsonModal"
      title="Interaction Details"
      size="lg"
      :show-footer="false"
      @close="closeJsonModal"
    >
      <pre class="json-viewer">{{ formattedJson }}</pre>
    </BaseModal>

    <!-- Public Key Modal -->
    <ServerPublicKeyModal
      :show="showKeyModal"
      :public-key="server?.publicKey || ''"
      :loading="isSavingKey"
      @close="closeKeyModal"
      @save="savePublicKey"
    />
  </div>
</template>

<script setup>
  import { ref, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import StatCard from '@/components/StatCard.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import BaseModal from '@/components/BaseModal.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import ServerPublicKeyModal from '@/components/ServerPublicKeyModal.vue';
  import { useServerDetails } from '@/composables/useServerDetails.js';
  import { serverService } from '@/services/serverService.js';
  import { notificationService } from '@/services/notificationService.js';
  import { formatStatus, getStatusTextClass } from '@/utils/serverStatus.js';
  import { getInteractionTypeBadge } from '@/utils/interactionTypes.js';
  import { truncateText, formatDateShort, formatTime, isValidJson } from '@/utils/stringUtils.js';

  const route = useRoute();

  const {
    server,
    loading,
    error,
    currentPage,
    filterType,
    searchQuery,
    showJsonModal,
    formattedJson,
    filteredInteractions,
    paginatedInteractions,
    totalPages,
    fetchServer,
    onPageChanged,
    clearFilters,
    openJsonModal,
    closeJsonModal,
  } = useServerDetails(route.params.id);

  const showKeyModal = ref(false);
  const isSavingKey = ref(false);

  function openKeyModal() {
    showKeyModal.value = true;
  }

  function closeKeyModal() {
    showKeyModal.value = false;
  }

  async function savePublicKey(newKey) {
    isSavingKey.value = true;
    try {
      await serverService.update(server.value.id, { publicKey: newKey });
      notificationService.success(
        'Public Key Saved',
        newKey ? 'The public key has been saved.' : 'The public key has been removed.'
      );
      closeKeyModal();
      await fetchServer();
    } catch (err) {
      console.error('Error saving public key:', err);
      notificationService.error(
        'Save Failed',
        err.response?.data?.detail ||
          err.response?.data?.message ||
          'Unable to save the public key. Please try again.'
      );
    } finally {
      isSavingKey.value = false;
    }
  }

  const interactionColumns = [
    { key: 'timestamp', label: 'Timestamp' },
    { key: 'type', label: 'Type', headerClass: 'center', cellClass: 'center' },
    { key: 'description', label: 'Description' },
  ];

  // Text color override for badge types that need it (e.g. bg-warning needs text-dark for contrast)
  function getInteractionBadgeTextColor(type) {
    const textColorByType = {
      REGISTRATION: 'text-dark', // bg-info needs dark text for adequate contrast
    };
    return textColorByType[type] || 'text-white';
  }

  onMounted(fetchServer);
</script>

<style scoped>
  .server-detail-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .page-actions {
    display: flex;
    justify-content: flex-start;
    margin-bottom: var(--spacing-md);
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: var(--spacing-md);
  }

  .filters-section {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: var(--spacing-md);
    align-items: flex-end;
  }

  .filter-item {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .filter-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--color-gray-500);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .filter-button-group {
    flex-direction: row;
    justify-content: flex-start;
  }

  .badge {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    padding: 0.35rem 0.65rem;
  }

  .json-viewer,
  .key-preview {
    background: var(--color-gray-50);
    border: 1px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    padding: var(--spacing-md);
    overflow-x: auto;
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
    line-height: 1.5;
    white-space: pre-wrap;
    word-wrap: break-word;
    margin: 0;
  }

  .alert {
    border: none;
    border-radius: var(--radius-md);
  }

  .alert-info {
    background-color: rgba(59, 130, 246, 0.1);
    color: #1e40af;
  }

  .alert-danger {
    background-color: rgba(239, 68, 68, 0.1);
    color: #991b1b;
  }

  .spinning {
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  @media (max-width: 768px) {
    .server-detail-page {
      padding: var(--spacing-md);
    }

    .filters-section {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 576px) {
    .server-detail-page {
      padding: var(--spacing-sm);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
