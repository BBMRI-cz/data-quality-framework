<template>
  <div class="d-flex gap-2 flex-wrap">
    <button
      class="btn btn-sm p-0 border-0 bg-transparent filter-chip"
      @click="$emit('update:modelValue', null)"
    >
      <Badge text="All" :variant="modelValue === null ? 'primary' : 'secondary'" size="large" />
    </button>
    <button
      v-for="category in categories"
      :key="category"
      class="btn btn-sm p-0 border-0 bg-transparent filter-chip"
      @click="$emit('update:modelValue', category)"
    >
      <Badge
        :text="category"
        :variant="modelValue === category ? 'primary' : 'secondary'"
        size="large"
      />
    </button>
  </div>
</template>

<script setup>
  import Badge from '@/components/ui/Badge.vue';

  defineProps({
    categories: {
      type: Array,
      required: true,
    },
    modelValue: {
      type: String,
      default: null,
    },
  });

  defineEmits(['update:modelValue']);
</script>

<style scoped>
  .filter-chip {
    line-height: 1;
  }
</style>
