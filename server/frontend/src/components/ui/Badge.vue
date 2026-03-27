<template>
  <span class="badge border rounded-pill" :style="badgeStyle" :class="badgeClasses">
    <span v-if="closable" class="badge-content">
      <slot>{{ text }}</slot>
      <button
        type="button"
        class="badge-close"
        aria-label="Remove badge"
        @click="emit('remove', text)"
      >
        <i class="bi bi-x"></i>
      </button>
    </span>
    <slot v-else>{{ text }}</slot>
  </span>
</template>

<script setup>
  import { computed } from 'vue';

  const emit = defineEmits(['remove']);

  const props = defineProps({
    text: {
      type: String,
      default: '',
    },
    variant: {
      type: String,
      default: 'primary',
      validator: (value) => ['primary', 'secondary'].includes(value),
    },
    color: {
      type: String,
      default: '',
    },
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value),
    },
    closable: {
      type: Boolean,
      default: false,
    },
  });

  const variantColors = {
    primary: 'var(--color-primary)',
    secondary: 'var(--color-gray-500)',
  };

  const activeColor = computed(
    () => props.color || variantColors[props.variant] || variantColors.primary
  );

  const badgeStyle = computed(() => ({
    backgroundColor: `color-mix(in srgb, ${activeColor.value} 12%, transparent)`,
    color: activeColor.value,
    borderColor: `color-mix(in srgb, ${activeColor.value} 25%, transparent)`,
  }));

  const badgeClasses = computed(() => ({
    'badge-sm': props.size === 'small',
    'badge-lg': props.size === 'large',
    'badge-closable': props.closable,
  }));
</script>

<style scoped>
  .badge {
    font-weight: 500;
    padding: 0.35rem 0.6rem;
    font-size: 0.813rem;
    white-space: nowrap;
    display: inline-flex;
    align-items: center;
    margin-right: 0.5rem;
    margin-bottom: 0.5rem;
    transition: all 0.3s ease;
  }

  .badge:last-child {
    margin-right: 0;
  }

  .badge-sm {
    padding: 0.25rem 0.5rem;
    font-size: 0.75rem;
    margin-right: 0.35rem;
    margin-bottom: 0.35rem;
  }

  .badge-lg {
    padding: 0.5rem 0.8rem;
    font-size: 0.875rem;
    margin-right: 0.6rem;
    margin-bottom: 0.6rem;
  }

  .badge-closable {
    padding-right: 0.8rem;
  }

  .badge-content {
    display: flex;
    align-items: center;
    gap: 0.4rem;
  }

  .badge-close {
    background: none;
    border: none;
    color: currentColor;
    opacity: 0.7;
    font-size: 1.3em;
    line-height: 1;
    cursor: pointer;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 1.25rem;
    height: 1.25rem;
    border-radius: 9999px;
    transition: all 0.2s ease;
  }

  .badge-close:hover {
    opacity: 1;
    background: color-mix(in srgb, currentColor 20%, transparent);
  }
</style>
