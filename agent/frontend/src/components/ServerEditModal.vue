<template>
  <BaseModal :show="true" @close="$emit('close')">
    <template #header>
      <h3>Edit Server</h3>
    </template>
    <template #body>
      <p>Edit modal content</p>
    </template>
    <template #footer>
      <button class="btn btn-secondary" @click="$emit('close')">Cancel</button>
      <button class="btn btn-primary" @click="$emit('submit', server)">Save</button>
    </template>
  </BaseModal>
</template>

<script setup>
  import BaseModal from './BaseModal.vue';

  defineProps({
    server: {
      type: Object,
      required: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  });

  defineEmits(['close', 'submit']);
</script>

<style scoped>
  .btn {
    padding: 0.75rem 1.5rem;
    font-size: 1rem;
    font-weight: 600;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-base);
  }

  .btn-secondary {
    background: var(--color-gray-200);
    color: var(--color-gray-700);
  }

  .btn-primary {
    background: var(--color-primary);
    color: white;
  }
</style>
