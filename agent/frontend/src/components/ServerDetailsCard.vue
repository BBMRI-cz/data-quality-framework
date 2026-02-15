<template>
  <div class="server-details-card">
    <div class="server-header">
      <h3 class="server-name">{{ server?.name || 'Unknown Server' }}</h3>
      <span
        :class="['status-badge-large', getStatusClass(server?.status)]"
        :title="getStatusTooltip(server?.status)"
      >
        <i :class="getStatusIcon(server?.status)"></i>
        <span class="status-text">{{ formatStatus(server?.status) }}</span>
      </span>
    </div>

    <div class="server-info-grid">
      <div class="info-item">
        <div class="info-label">
          <i class="bi bi-link-45deg"></i>
          Server URL
        </div>
        <div class="info-value">
          <a
            v-if="server?.url"
            :href="server.url"
            target="_blank"
            rel="noopener noreferrer"
            class="server-link"
          >
            {{ server.url }}
            <i class="bi bi-box-arrow-up-right"></i>
          </a>
          <span v-else>N/A</span>
        </div>
      </div>
    </div>

    <div class="server-actions-bar">
      <button class="btn btn-primary" @click="handleViewDetails">
        <i class="bi bi-info-circle"></i>
        View Details
      </button>
      <button class="btn btn-delete" @click="handleDelete">
        <i class="bi bi-trash"></i>
        Remove
      </button>
    </div>
  </div>
</template>

<script setup>
  import {
    getStatusClass,
    getStatusIcon,
    formatStatus,
    getStatusTooltip,
  } from '@/utils/serverStatus.js';

  const props = defineProps({
    server: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['delete', 'viewDetails']);

  function handleViewDetails() {
    emit('viewDetails', props.server);
  }

  function handleDelete() {
    emit('delete', props.server);
  }
</script>

<style scoped>
  .server-details-card {
    background: var(--color-gray-50);
    border-radius: var(--radius-lg);
    padding: var(--spacing-2xl);
    border: 2px solid var(--color-gray-100);
  }

  .server-header {
    margin-bottom: var(--spacing-xl);
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: var(--spacing-lg);
  }

  .server-name {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--color-gray-800);
    margin: 0;
    flex: 1;
  }

  .status-badge-large {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-sm);
    padding: 0.625rem 1.25rem;
    border-radius: var(--radius-md);
    font-size: 0.95rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    box-shadow: var(--shadow-sm);
    border: 2px solid transparent;
    transition: all var(--transition-base);
    cursor: help;
  }

  .status-badge-large i {
    font-size: 1.1rem;
  }

  .status-badge-large.status-active {
    background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
    color: #065f46;
    border-color: #10b981;
  }

  .status-badge-large.status-inactive {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    color: #92400e;
    border-color: #f59e0b;
  }

  .status-badge-large.status-error {
    background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
    color: #991b1b;
    border-color: #ef4444;
  }

  .status-badge-large.status-pending {
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
    color: #7a5800;
    border-color: #f59e0b;
  }

  .status-badge-large.status-unknown {
    background: linear-gradient(135deg, var(--color-gray-100) 0%, var(--color-gray-200) 100%);
    color: var(--color-gray-800);
    border-color: var(--color-gray-400);
  }

  .server-info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: var(--spacing-xl);
    margin-bottom: var(--spacing-2xl);
  }

  .info-item {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);
  }

  .info-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-xs);
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--color-gray-600);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .info-label i {
    color: var(--color-primary);
  }

  .info-value {
    font-size: 1.1rem;
    font-weight: 600;
    color: var(--color-gray-800);
    word-break: break-word;
  }

  .server-link {
    color: var(--color-primary);
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-sm);
    transition: color var(--transition-base);
  }

  .server-link:hover {
    color: var(--color-primary-dark);
    text-decoration: underline;
  }

  .server-actions-bar {
    display: flex;
    gap: var(--spacing-md);
    padding-top: var(--spacing-xl);
    border-top: 2px solid var(--color-gray-200);
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

  .btn-primary {
    background: var(--color-primary);
    color: white;
  }

  .btn-primary:hover {
    background: var(--color-primary-dark);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .btn-delete {
    background: var(--color-danger);
    color: white;
  }

  .btn-delete:hover {
    background: #b91c1c;
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  @media (max-width: 768px) {
    .server-details-card {
      padding: var(--spacing-lg);
    }

    .server-header {
      flex-direction: column;
      align-items: flex-start;
    }

    .server-info-grid {
      grid-template-columns: 1fr;
    }

    .server-actions-bar {
      flex-direction: column;
    }
  }
</style>
