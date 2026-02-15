<template>
  <div class="form-row" :class="rowClasses">
    <slot />
  </div>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    cols: {
      type: [Number, String],
      default: 2,
    },
    gap: {
      type: String,
      default: 'md',
      validator: (value) => ['sm', 'md', 'lg'].includes(value),
    },
  });

  const rowClasses = computed(() => [
    `form-row-cols-${props.cols}`,
    `form-row-gap-${props.gap}`,
  ]);
</script>

<style scoped>
  .form-row {
    display: grid;
    grid-template-columns: repeat(1, 1fr);
  }

  .form-row-cols-2 {
    grid-template-columns: repeat(2, 1fr);
  }

  .form-row-cols-3 {
    grid-template-columns: repeat(3, 1fr);
  }

  .form-row-cols-4 {
    grid-template-columns: repeat(4, 1fr);
  }

  .form-row-gap-sm {
    gap: var(--spacing-sm);
  }

  .form-row-gap-md {
    gap: var(--spacing-md);
  }

  .form-row-gap-lg {
    gap: var(--spacing-lg);
  }

  /* Responsive: stack on smaller screens */
  @media (max-width: 768px) {
    .form-row-cols-2,
    .form-row-cols-3,
    .form-row-cols-4 {
      grid-template-columns: 1fr;
    }
  }
</style>

