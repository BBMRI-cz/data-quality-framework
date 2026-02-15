<template>
  <div
    class="notification-toast"
    :class="toastClass"
    role="alert"
    aria-live="assertive"
    aria-atomic="true"
  >
    <div class="notification-content">
      <div class="notification-icon">
        <i :class="iconClass"></i>
      </div>
      <div class="notification-text">
        <div class="notification-title">{{ title }}</div>
        <div v-if="message" class="notification-message">{{ message }}</div>
      </div>
      <button
        type="button"
        class="notification-close"
        aria-label="Close"
        @click="closeNotification"
      >
        <i class="bi bi-x-lg"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
  import { computed, onMounted, onUnmounted } from 'vue';

  const props = defineProps({
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['success', 'error', 'warning', 'info'].includes(value),
    },
    title: {
      type: String,
      required: true,
    },
    message: {
      type: String,
      default: '',
    },
    duration: {
      type: Number,
      default: 5000,
    },
    autoClose: {
      type: Boolean,
      default: true,
    },
  });

  const emit = defineEmits(['close']);

  const toastClass = computed(() => {
    switch (props.type) {
      case 'success':
        return 'notification-success';
      case 'error':
        return 'notification-error';
      case 'warning':
        return 'notification-warning';
      default:
        return 'notification-info';
    }
  });

  const iconClass = computed(() => {
    switch (props.type) {
      case 'success':
        return 'bi bi-check-circle-fill';
      case 'error':
        return 'bi bi-bug-fill';
      case 'warning':
        return 'bi bi-exclamation-triangle-fill';
      default:
        return 'bi bi-info-circle-fill';
    }
  });

  let timeoutId = null;

  const closeNotification = () => {
    emit('close');
  };

  onMounted(() => {
    if (props.autoClose && props.duration > 0) {
      timeoutId = setTimeout(() => {
        closeNotification();
      }, props.duration);
    }
  });

  onUnmounted(() => {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
  });
</script>

<style scoped>
  .notification-toast {
    display: flex;
    width: 100%;
    margin-bottom: var(--spacing-sm);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-md);
    border-left-width: 4px;
    border-left-style: solid;
    overflow: hidden;
    transition: all var(--transition-base);
  }

  .notification-toast.notification-success {
    background-color: #d1e7dd;
    border-left-color: var(--color-success);
    color: #0f5132;
  }

  .notification-toast.notification-error {
    background-color: #f8d7da;
    border-left-color: var(--color-danger);
    color: #842029;
  }

  .notification-toast.notification-warning {
    background-color: #fff3cd;
    border-left-color: var(--color-warning);
    color: #664d03;
  }

  .notification-toast.notification-info {
    background-color: #cff4fc;
    border-left-color: var(--color-info);
    color: #055160;
  }

  .notification-content {
    display: flex;
    align-items: flex-start;
    width: 100%;
    padding: var(--spacing-md) var(--spacing-lg);
    gap: var(--spacing-md);
  }

  .notification-icon {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.25rem;
    margin-top: 2px;
  }

  .notification-text {
    flex: 1;
    min-width: 0;
  }

  .notification-title {
    font-size: 0.95rem;
    font-weight: 600;
    line-height: 1.4;
    margin-bottom: var(--spacing-xs);
  }

  .notification-message {
    font-size: 0.875rem;
    line-height: 1.5;
    opacity: 0.9;
    word-wrap: break-word;
    overflow-wrap: break-word;
    white-space: pre-wrap;
  }

  .notification-close {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    background: transparent;
    border: none;
    border-radius: var(--radius-sm);
    color: inherit;
    opacity: 0.6;
    cursor: pointer;
    transition:
      opacity var(--transition-fast),
      background-color var(--transition-fast);
  }

  .notification-close:hover {
    opacity: 1;
    background-color: rgba(0, 0, 0, 0.1);
  }

  .notification-close i {
    font-size: 1rem;
  }

  @media (max-width: 768px) {
    .notification-content {
      padding: var(--spacing-md);
      gap: var(--spacing-sm);
    }

    .notification-title {
      font-size: 0.9rem;
    }

    .notification-message {
      font-size: 0.813rem;
    }

    .notification-close {
      width: 36px;
      height: 36px;
    }
  }
</style>
