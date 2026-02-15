<template>
  <div class="form-card">
    <form v-if="!noForm" @submit.prevent="$emit('submit')">
      <slot />
    </form>
    <template v-else>
      <slot />
    </template>
  </div>
</template>

<script setup>
  defineProps({
    noForm: {
      type: Boolean,
      default: false,
    },
  });

  defineEmits(['submit']);
</script>

<style scoped>
  .form-card {
    background: var(--bg-card);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
  }

  .form-card form {
    display: contents;
  }
</style>

