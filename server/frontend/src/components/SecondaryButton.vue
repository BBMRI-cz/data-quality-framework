<template>
  <button :type="type" class="btn-secondary" :disabled="disabled" v-bind="$attrs">
    <i v-if="icon" :class="['bi', icon, 'me-2']"></i>
    <slot>{{ text }}</slot>
  </button>
</template>

<script setup>
  defineProps({
    type: {
      type: String,
      default: 'button',
    },
    disabled: {
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
  .btn-secondary {
    background: transparent;
    border: 1px solid var(--color-gray-300);
    color: var(--color-gray-600);
    padding: calc(var(--spacing-sm) * 0.8) calc(var(--spacing-lg) * 0.8);
    border-radius: var(--radius-sm);
    font-weight: 500;
    font-size: 0.8rem;
    transition: all var(--transition-base);
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .btn-secondary:hover:not(:disabled) {
    background: var(--color-gray-100);
    border-color: var(--color-gray-300);
    color: var(--color-gray-700);
    transform: translateY(-1px);
  }

  .btn-secondary:active:not(:disabled) {
    transform: translateY(0);
  }

  .btn-secondary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-secondary:focus {
    outline: 2px solid rgba(107, 114, 128, 0.35);
    outline-offset: 2px;
  }
</style>
