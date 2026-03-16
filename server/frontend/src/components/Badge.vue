<template>
  <span class="badge" :class="badgeClasses">
    <span v-if="closable" class="badge-content">
      <slot>{{ text }}</slot>
      <button class="badge-close" @click="$emit('remove', text)">
        <i class="bi bi-x"></i>
      </button>
    </span>
    <slot v-else>{{ text }}</slot>
  </span>
</template>

<script setup>
  import { computed } from 'vue';

  defineEmits(['remove']);

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
    closable: {
      type: Boolean,
      default: false,
    },
  });

  const badgeClasses = computed(() => {
    const classes = [`bg-${props.variant}`];
    if (props.size === 'small') {
      classes.push('badge-sm');
    } else if (props.size === 'large') {
      classes.push('badge-lg');
    }
    if (props.closable) {
      classes.push('badge-closable');
    }
    return classes;
  });
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
  }

  .badge-close {
    background: none;
    border: none;
    color: rgba(255, 255, 255, 0.7);
    font-size: 1.3em;
    line-height: 1;
    cursor: pointer;
    padding: 0;
    margin-left: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 1.25rem;
    height: 1.25rem;
    border-radius: 9999px;
    transition: all 0.2s ease;
  }

  .badge-close:hover {
    color: white;
    background: rgba(255, 255, 255, 0.2);
  }
</style>