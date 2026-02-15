<template>
  <BaseModal :show="true" :show-footer="false" @close="$emit('close')">
    <template #header>
      <div class="modal-header-content">
        <h3 class="mb-0">Remove Server Connection</h3>
      </div>
    </template>

    <!-- Default slot for body content -->
    <div class="modal-body-content">
      <p class="lead-text">
        Are you sure you want to remove the connection to <strong>{{ itemName }}</strong
        >?
      </p>

      <div class="alert alert-info">
        <i class="bi bi-info-circle me-2"></i>
        <div class="alert-content">
          <strong>What this means:</strong>
          <ul class="mb-0 mt-2">
            <li>New reports will no longer be sent to this central server</li>
            <li>Reports already sent to this server will remain there</li>
            <li>You can re-register this server again later if needed</li>
          </ul>
        </div>
      </div>

      <p class="text-muted mb-0">
        <small
          >This action will only remove the connection configuration from this agent. The central
          server and its data will not be affected.</small
        >
      </p>

      <!-- Custom Footer inside body -->
      <div class="modal-footer-custom">
        <button class="btn btn-secondary" :disabled="loading" @click="$emit('close')">
          Cancel
        </button>
        <button class="btn btn-danger" :disabled="loading" @click="$emit('confirm')">
          <span v-if="loading">
            <span
              class="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            ></span>
            Removing...
          </span>
          <span v-else>
            <i class="bi bi-trash me-2"></i>
            Remove Connection
          </span>
        </button>
      </div>
    </div>
  </BaseModal>
</template>

<script setup>
  import BaseModal from './BaseModal.vue';

  defineProps({
    itemName: {
      type: String,
      required: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  });

  defineEmits(['close', 'confirm']);
</script>

<style scoped>
  .modal-body-content {
    padding: 0.5rem 0;
  }

  .modal-footer-custom {
    display: flex;
    gap: 0.75rem;
    justify-content: center;
    margin-top: 1.5rem;
    padding-top: 1rem;
    border-top: 1px solid var(--color-gray-200);
  }

  .btn {
    padding: 0.75rem 1.5rem;
    font-size: 1rem;
    font-weight: 600;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-base);
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: var(--color-gray-200);
    color: var(--color-gray-700);
  }

  .btn-secondary:hover:not(:disabled) {
    background: var(--color-gray-300);
  }

  .btn-danger {
    background: var(--color-danger);
    color: white;
  }

  .btn-danger:hover:not(:disabled) {
    background: #b91c1c;
  }

  .lead-text {
    font-size: 1.1rem;
    margin-bottom: 1.5rem;
    color: var(--color-gray-800);
  }

  .alert {
    padding: 1rem;
    border-radius: var(--radius-md);
    margin-bottom: 1rem;
    display: flex;
    gap: 0.5rem;
  }

  .alert-info {
    background-color: #dbeafe;
    border: 1px solid #93c5fd;
    color: #1e40af;
  }

  .alert-content {
    flex: 1;
  }

  .alert-content ul {
    padding-left: 1.25rem;
    margin-top: 0.5rem;
  }

  .alert-content li {
    margin-bottom: 0.25rem;
  }

  .text-warning {
    color: #f59e0b;
  }

  .spinner-border {
    width: 1rem;
    height: 1rem;
    border-width: 0.15em;
  }

  .modal-header-content {
    width: 100%;
  }
</style>
