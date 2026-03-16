<template>
  <span class="badge" :class="badgeClasses">
    <slot>{{ text }}</slot>
  </span>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    text: {
      type: String,
      default: '',
    },
    variant: {
      type: String,
      default: 'primary',
      validator: (value) =>
        ['primary', 'secondary', 'success', 'danger', 'warning', 'info', 'light', 'dark'].includes(
          value
        ),
    },
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value),
    },
  });

  const badgeClasses = computed(() => {
    const classes = [`bg-${props.variant}`];
    if (props.size === 'small') {
      classes.push('badge-sm');
    } else if (props.size === 'large') {
      classes.push('badge-lg');
    }
    return classes;
  });
</script>

<style scoped>
  .badge {
    font-weight: 500;
    padding: 0.5rem 0.75rem;
    font-size: 0.813rem;
    white-space: nowrap;
    display: inline-block;
    margin-right: 0.35rem;
    margin-bottom: 0.35rem;
  }

  .badge:last-child {
    margin-right: 0;
  }

  .badge-sm {
    padding: 0.35rem 0.65rem;
    font-size: 0.75rem;
    margin-right: 0.25rem;
    margin-bottom: 0.25rem;
  }

  .badge-lg {
    padding: 0.6rem 0.9rem;
    font-size: 0.875rem;
    margin-right: 0.6rem;
    margin-bottom: 0.6rem;
  }
</style>