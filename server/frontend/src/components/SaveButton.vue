<template>
  <button
    :type="type"
    class="btn btn-save-gradient"
    :disabled="disabled || loading"
    v-bind="$attrs"
  >
    <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"></span>
    <i v-else-if="icon" :class="['bi', icon, 'me-2']"></i>
    <slot>{{ text }}</slot>
  </button>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'button'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  icon: {
    type: String,
    default: 'bi-check-circle'
  },
  text: {
    type: String,
    default: 'Save'
  }
})

// Allow all attributes to be passed through
defineOptions({
  inheritAttrs: false
})
</script>

<style scoped>
.btn-save-gradient {
  background: var(--color-primary);
  color: white;
  border: none;
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-sm);
  font-weight: 600;
  transition: all var(--transition-slow);
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-save-gradient:hover:not(:disabled) {
  background: var(--color-primary);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.25);
  color: white;
}

.btn-save-gradient:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.2);
}

.btn-save-gradient:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-save-gradient:focus {
  outline: 2px solid rgba(102, 126, 234, 0.5);
  outline-offset: 2px;
}
</style>
