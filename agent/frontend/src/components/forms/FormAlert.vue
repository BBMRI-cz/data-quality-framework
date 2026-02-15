<template>
  <div v-if="show" class="form-alert" :class="`form-alert-${variant}`" role="alert">
    <i :class="['bi', alertIcon, 'me-2']"></i>
    <span class="alert-message">
      <slot>{{ message }}</slot>
    </span>
    <button v-if="dismissible" type="button" class="alert-close" @click="$emit('dismiss')">
      <i class="bi bi-x"></i>
    </button>
  </div>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    show: {
      type: Boolean,
      default: true,
    },
    variant: {
      type: String,
      default: 'info',
      validator: (value) => ['success', 'danger', 'warning', 'info'].includes(value),
    },
    message: {
      type: String,
      default: '',
    },
    icon: {
      type: String,
      default: '',
    },
    dismissible: {
      type: Boolean,
      default: false,
    },
  });

  defineEmits(['dismiss']);

  const alertIcon = computed(() => {
    if (props.icon) return props.icon;

    const icons = {
      success: 'bi-check-circle',
      danger: 'bi-exclamation-circle',
      warning: 'bi-exclamation-triangle',
      info: 'bi-info-circle',
    };
    return icons[props.variant];
  });
</script>

<style scoped>
  .form-alert {
    display: flex;
    align-items: center;
    padding: 0.75rem var(--spacing-md);
    border-radius: var(--radius-md);
    font-size: 0.875rem;
    margin-bottom: var(--spacing-md);
  }

  .form-alert-success {
    background-color: #d1fae5;
    color: #059669;
  }

  .form-alert-danger {
    background-color: #fee2e2;
    color: var(--color-danger);
  }

  .form-alert-warning {
    background-color: #fef3c7;
    color: #d97706;
  }

  .form-alert-info {
    background-color: #dbeafe;
    color: #2563eb;
  }

  .alert-message {
    flex: 1;
  }

  .alert-close {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    margin-left: var(--spacing-sm);
    color: inherit;
    opacity: 0.7;
    transition: opacity var(--transition-base);
  }

  .alert-close:hover {
    opacity: 1;
  }
</style>

