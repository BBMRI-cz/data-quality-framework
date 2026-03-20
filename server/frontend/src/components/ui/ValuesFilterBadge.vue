<template>
  <span v-if="category" class="badge border" :style="categoryStyle">
    {{ category.name }}
  </span>
  <span v-else class="badge border" :style="defaultStyle"> No Category </span>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    category: {
      type: Object,
      default: null,
    },
  });

  const categoryStyle = computed(() => {
    if (!props.category) return {};
    const color = props.category.colorHex || '#6c757d';
    return {
      backgroundColor: `${color}20`,
      color: color,
      borderColor: `${color}40`,
    };
  });

  const defaultStyle = {
    backgroundColor: '#6c757d20',
    color: '#6c757d',
    borderColor: '#6c757d40',
  };
</script>

<style scoped>
  .badge {
    font-weight: 500;
    padding: 0.35rem 0.65rem;
    font-size: 0.75rem;
    white-space: nowrap;
  }
</style>
