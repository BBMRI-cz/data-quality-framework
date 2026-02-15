<template>
  <component
    :is="to ? 'router-link' : 'button'"
    :to="to"
    :type="to ? undefined : type"
    :disabled="disabled || loading"
    class="action-button"
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
  background: var(--color-success);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-slow);
  box-shadow: 0 2px 4px rgba(25, 135, 84, 0.2);
}

.action-button:hover:not(:disabled) {
  background: var(--color-success);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(25, 135, 84, 0.25);
  color: white;
}

.action-button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(25, 135, 84, 0.2);
}

.action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-button:focus {
  outline: 2px solid rgba(25, 135, 84, 0.5);
  outline-offset: 2px;
}

.spinner-border-sm {
  width: 1rem;
  height: 1rem;
  border-width: 0.15em;
}
</style>

