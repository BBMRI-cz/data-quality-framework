<template>
  <span class="category-badge" :style="badgeStyle">
    {{ category?.name || 'No Category' }}
  </span>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    category: {
      type: Object,
      default: null,
    },
  });

  const badgeStyle = computed(() => {
    const color = props.category?.colorHex || '#6b7280';
    return {
      backgroundColor: `${color}15`,
      borderColor: `${color}40`,
      color: color,
    };
  });
</script>

<style scoped>
  .category-badge {
    display: inline-flex;
    align-items: center;
    padding: 0.25rem 0.75rem;
    border-radius: var(--radius-full);
    border: 1px solid;
    font-size: 0.75rem;
    font-weight: 600;
    line-height: 1.4;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
