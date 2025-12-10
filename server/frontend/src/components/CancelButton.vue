<template>
  <button
    :type="type"
    class="btn btn-cancel"
    :disabled="disabled"
    v-bind="$attrs"
  >
    <i v-if="icon" :class="['bi', icon, 'me-2']"></i>
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
  icon: {
    type: String,
    default: 'bi-x-circle'
  },
  text: {
    type: String,
    default: 'Cancel'
  }
})

// Allow all attributes to be passed through
defineOptions({
  inheritAttrs: false
})
</script>

<style scoped>
.btn-cancel {
  background: var(--color-gray-500);
  border-color: var(--color-gray-500);
  color: white;
  border: none;
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all var(--transition-base);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-cancel:hover:not(:disabled) {
  background: var(--color-gray-600);
  border-color: var(--color-gray-600);
  transform: translateY(-1px);
  color: white;
}

.btn-cancel:active:not(:disabled) {
  transform: translateY(0);
}

.btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel:focus {
  outline: 2px solid rgba(107, 114, 128, 0.5);
  outline-offset: 2px;
}
</style>
