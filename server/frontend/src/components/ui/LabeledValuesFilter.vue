<template>
  <div>
    <div class="filter-label">{{ label }}</div>
    <ValuesFilter :model-value="modelValue" :categories="categories" @update:model-value="onUpdate" />
  </div>
</template>

<script setup>
  import ValuesFilter from '@/components/ui/ValuesFilter.vue';

  defineProps({
    label: {
      type: String,
      required: true,
    },
    categories: {
      type: Array,
      required: true,
    },
    modelValue: {
      type: String,
      default: null,
    },
  });

  const emit = defineEmits(['update:modelValue']);

  const onUpdate = (value) => {
    emit('update:modelValue', value);
  };
</script>

<style scoped>
  .filter-label {
    font-size: 0.875rem;
    color: var(--color-gray-500);
    font-weight: 500;
    margin-bottom: 0.5rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
</style>

