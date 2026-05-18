<template>
  <div class="d-flex justify-content-end align-items-center gap-2">
    <SecondaryButton
      class="btn-sm"
      :disabled="isPreviousDisabled"
      icon="bi-arrow-left"
      text="Previous"
      aria-label="Previous page"
      @click="$emit('previous', $event)"
    />
    <span class="small text-muted">Page {{ page + 1 }} of {{ totalPages }}</span>
    <SecondaryButton
      class="btn-sm"
      :disabled="isNextDisabled"
      icon="bi-arrow-right"
      text="Next"
      aria-label="Next page"
      @click="$emit('next', $event)"
    />
  </div>
</template>

<script setup>
  import { computed } from 'vue';
  import SecondaryButton from '@/components/SecondaryButton.vue';

  const props = defineProps({
    page: {
      type: Number,
      required: true,
    },
    totalPages: {
      type: Number,
      required: true,
    },
  });

  defineEmits(['previous', 'next']);

  const isPreviousDisabled = computed(() => props.page <= 0);
  const isNextDisabled = computed(() => props.page >= props.totalPages - 1);
</script>
