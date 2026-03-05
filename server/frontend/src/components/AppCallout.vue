<template>
  <div :class="['alert', `alert-${type}`, 'app-callout', dense ? 'py-2 px-3' : 'py-3 px-4']">
    <div class="d-flex align-items-start gap-2">
      <i v-if="computedIcon" :class="['bi', computedIcon, 'me-1', 'flex-shrink-0']"></i>
      <div class="flex-grow-1">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    type: {
      type: String,
      default: 'info', // info | warning | success | danger | secondary
    },
    icon: {
      type: String,
      default: '', // e.g. 'bi-shield-lock'
    },
    dense: {
      type: Boolean,
      default: true,
    },
  });

  const typeToIcon = {
    info: 'bi-info-circle',
    warning: 'bi-exclamation-triangle',
    success: 'bi-check-circle',
    danger: 'bi-x-circle',
    secondary: 'bi-info-circle',
  };

  const computedIcon = computed(() => props.icon || typeToIcon[props.type] || '');
</script>

<style scoped>
  .app-callout {
    border: 1px solid var(--bs-border-color, #e9ecef);
    border-left-width: 4px;
    border-radius: 0.5rem;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    font-size: 0.95rem;
  }

  .alert-info {
    border-left-color: #0dcaf0; /* Bootstrap info */
  }

  .alert-warning {
    border-left-color: #ffc107;
  }

  .alert-danger {
    border-left-color: #dc3545;
  }

  .alert-success {
    border-left-color: #198754;
  }

  .app-callout :deep(a) {
    text-decoration: underline;
  }
</style>
