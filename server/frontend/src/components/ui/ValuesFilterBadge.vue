<template>
  <Badge :text="label" :color="activeColor" size="small" />
</template>

<script setup>
  import { computed } from 'vue';
  import Badge from '@/components/ui/Badge.vue';

  const props = defineProps({
    category: {
      type: Object,
      default: null,
    },
  });

  const label = computed(() => props.category?.name || 'No Category');
  const activeColor = computed(() => props.category?.colorHex || '#6c757d');
</script>
