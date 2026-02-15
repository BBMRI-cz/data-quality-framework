<template>
  <div class="server-detail-page">
    <PageHeader
      :title="server?.name || 'Server Details'"
      :mobile-title="server?.name || 'Server'"
      subtitle="Server configuration and interaction history"
      icon="bi bi-server"
    />

    <div class="page-content">
      <!-- Action Section -->
      <div class="page-actions">
        <ActionButton
          to="/servers"
          icon="bi bi-arrow-left"
          text="Back to Servers"
          variant="secondary"
        />
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Loading server details...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="error-state">
        <i class="bi bi-exclamation-triangle"></i>
        <p>{{ error }}</p>
      </div>

      <!-- Server details -->
      <template v-else-if="server">
        <!-- Stats Cards -->
        <div class="stats-grid">
          <StatCard
            :number="server.name"
            label="Server Name"
            number-class="text-dark"
          />
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
          <StatCard
            :number="server.clientId || 'N/A'"
            label="Client ID"
            number-class="text-dark"
          />
        </div>

        <!-- Server URL Card -->
        <div class="url-card">
          <div class="url-label">
            <i class="bi bi-link-45deg"></i>
            Server URL
          </div>
          <a
            :href="server.url"
            target="_blank"
            rel="noopener noreferrer"
            class="url-value"
          >
            {{ server.url }}
            <i class="bi bi-box-arrow-up-right"></i>
          </a>
        </div>

        <!-- Filters -->
        <div class="filters-section">
          <div class="filter-group">
            <label class="filter-label">Filter by Type</label>
            <select v-model="filterType" class="filter-select">
              <option value="">All Types</option>
              <option value="UPDATE">Update</option>
              <option value="COMMUNICATION">Communication</option>
              <option value="REGISTRATION">Registration</option>
            </select>
          </div>
          <div class="filter-group">
            <label class="filter-label">Search Description</label>
            <input
              v-model="searchQuery"
              type="text"
              class="filter-input"
              placeholder="Search in descriptions..."
            />
          </div>
          <div class="filter-group">
            <label class="filter-label">&nbsp;</label>
            <button
              class="clear-filters-btn"
              :disabled="!filterType && !searchQuery"
              @click="clearFilters"
            >
              <i class="bi bi-x-circle"></i>
              Clear Filters
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
          item-key="id"
          item-label="interactions"
          empty-text="No interactions logged yet"
          empty-icon="bi bi-inbox"
          @page-change="onPageChanged"
        >
          <template #timestamp="{ item }">
            <div class="timestamp-cell">
              <span class="timestamp-date">{{ formatDateShort(item.timestamp) }}</span>
              <span class="timestamp-time">{{ formatTime(item.timestamp) }}</span>
            </div>
          </template>
          <template #type="{ item }">
            <span class="type-badge" :class="getInteractionTypeClass(item.type)">
              {{ item.type }}
            </span>
          </template>
          <template #description="{ item }">
            <div class="description-cell">
              <span>{{ truncateText(item.description, 60) }}</span>
              <button
                v-if="isValidJson(item.description)"
                class="json-btn"
                title="View JSON details"
                @click.stop="openJsonModal(item.description)"
              >
                <i class="bi bi-braces"></i>
                View JSON
              </button>
            </div>
          </template>
        </BaseTable>
      </template>
    </div>

    <!-- Delete Confirmation Modal -->
    <DeleteConfirmModal
      v-if="showDeleteModal"
      :item-name="server?.name || 'this server'"
      :loading="isDeleting"
      @close="closeDeleteModal"
      @confirm="deleteServer"
    />

    <!-- JSON Viewer Modal -->
    <div v-if="showJsonModal" class="modal-overlay" @click="closeJsonModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>Interaction Details</h5>
          <button class="modal-close" @click="closeJsonModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <pre class="json-viewer">{{ formattedJson }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRoute } from 'vue-router';
import PageHeader from '@/components/PageHeader.vue';
import StatCard from '@/components/StatCard.vue';
import BaseTable from '@/components/BaseTable.vue';
import ActionButton from '@/components/ActionButton.vue';
import DeleteConfirmModal from '@/components/DeleteConfirmModal.vue';
import { useServerDetails } from '@/composables/useServerDetails.js';
import { formatStatus, getStatusTextClass } from '@/utils/serverStatus.js';
import { getInteractionTypeClass } from '@/utils/interactionTypes.js';
import { truncateText, formatDateShort, formatTime, isValidJson } from '@/utils/stringUtils.js';

const route = useRoute();

const {
  server,
  loading,
  error,
  currentPage,
  filterType,
  searchQuery,
  showDeleteModal,
  isDeleting,
  showJsonModal,
  formattedJson,
  filteredInteractions,
  paginatedInteractions,
  totalPages,
  fetchServer,
  deleteServer,
  onPageChanged,
  clearFilters,
  openJsonModal,
  closeJsonModal,
  closeDeleteModal,
} = useServerDetails(route.params.id);

const interactionColumns = [
  { key: 'timestamp', label: 'Timestamp' },
  { key: 'type', label: 'Type', headerClass: 'center', cellClass: 'center' },
  { key: 'description', label: 'Description' },
];

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
  margin-bottom: var(--spacing-lg);
}

.url-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--spacing-lg);
}

.url-label {
  font-size: 0.813rem;
  color: var(--color-gray-500);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--spacing-sm);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.url-value {
  font-size: 1rem;
  color: var(--color-primary);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  word-break: break-all;
}

.url-value:hover {
  text-decoration: underline;
}

.filters-section {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  min-width: 180px;
  flex: 1;
}

.filter-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-gray-500);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-select,
.filter-input {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  background: var(--bg-card);
}

.filter-select:focus,
.filter-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.clear-filters-btn {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--color-gray-600);
  font-size: 0.875rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  transition: all var(--transition-base);
}

.clear-filters-btn:hover:not(:disabled) {
  background: var(--color-gray-100);
  border-color: var(--color-gray-400);
}

.clear-filters-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.timestamp-cell {
  display: flex;
  flex-direction: column;
}

.timestamp-date {
  font-weight: 500;
}

.timestamp-time {
  font-size: 0.75rem;
  color: var(--color-gray-500);
}

.type-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.type-update {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.type-communication {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.type-registration {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.description-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.json-btn {
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-primary);
  font-size: 0.75rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  white-space: nowrap;
  transition: all var(--transition-base);
}

.json-btn:hover {
  background: var(--color-primary);
  color: white;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 0;
  color: var(--color-gray-500);
}

.spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid var(--color-gray-200);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-lg);
  color: var(--color-danger);
}

.error-state i {
  font-size: 2rem;
  margin-bottom: var(--spacing-sm);
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: var(--spacing-lg);
}

.modal-content {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  max-width: 800px;
  width: 100%;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-gray-200);
}

.modal-header h5 {
  margin: 0;
  font-weight: 600;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: var(--color-gray-500);
  cursor: pointer;
  padding: var(--spacing-xs);
}

.modal-close:hover {
  color: var(--color-gray-700);
}

.modal-body {
  padding: var(--spacing-lg);
  overflow: auto;
}

.json-viewer {
  background: var(--color-gray-50);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  overflow-x: auto;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}

@media (max-width: 768px) {
  .server-detail-page {
    padding: var(--spacing-md);
  }

  .filters-section {
    flex-direction: column;
  }

  .filter-group {
    width: 100%;
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
