<template>
  <component
    :is="to ? 'router-link' : 'button'"
    :to="to"
    :type="to ? undefined : type"
    :disabled="disabled || loading"
    class="action-button"
    :class="[`action-button--${variant}`]"
    v-bind="$attrs"
  >
    <span
      v-if="loading"
      class="spinner-border spinner-border-sm"
      role="status"
      aria-hidden="true"
    ></span>
    <i v-else-if="icon" :class="icon"></i>
    <slot>{{ text }}</slot>
  </component>
</template>

<script setup>
defineProps({
  to: {
    type: [String, Object],
    default: null,
  },
  type: {
    type: String,
    default: 'button',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  icon: {
    type: String,
    default: '',
  },
  text: {
    type: String,
    default: '',
  },
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary'].includes(value),
  },
});

defineOptions({
  inheritAttrs: false,
});
</script>

<style scoped>
.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-base);
}

.action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Primary variant (default) */
.action-button--primary {
  background: var(--color-success);
  color: white;
  border: none;
  box-shadow: 0 2px 4px rgba(25, 135, 84, 0.2);
}

.action-button--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(25, 135, 84, 0.25);
  color: white;
}

.action-button--primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(25, 135, 84, 0.2);
}

.action-button--primary:focus {
  outline: 2px solid rgba(25, 135, 84, 0.5);
  outline-offset: 2px;
}

/* Secondary variant (outline) */
.action-button--secondary {
  background: transparent;
  color: var(--color-gray-600);
  border: 1px solid var(--color-gray-300);
}

.action-button--secondary:hover:not(:disabled) {
  background: var(--color-gray-100);
  border-color: var(--color-gray-400);
  color: var(--color-gray-800);
}

.action-button--secondary:focus {
  outline: 2px solid rgba(108, 117, 125, 0.5);
  outline-offset: 2px;
}

.spinner-border-sm {
  width: 1rem;
  height: 1rem;
  border-width: 0.15em;
}
</style>

