<template>
  <div class="d-flex gap-2 flex-wrap">
    <button
      class="btn btn-sm rounded-pill"
      :class="modelValue === null ? 'btn-custom-primary' : 'btn-outline-custom-primary'"
      @click="$emit('update:modelValue', null)"
    >
      All
    </button>
    <button
      v-for="element in elements"
      :key="element"
      class="btn btn-sm rounded-pill"
      :class="modelValue === element ? 'btn-custom-primary' : 'btn-outline-custom-primary'"
      @click="$emit('update:modelValue', element)"
    >
      {{ element }}
    </button>
  </div>
</template>

<script setup>
  defineProps({
    elements: {
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
  .btn-custom-primary {
    background-color: var(--color-primary);
    border-color: var(--color-primary);
    color: #fff;
  }

  .btn-custom-primary:hover {
    background-color: var(--color-primary-dark);
    border-color: var(--color-primary-dark);
    color: #fff;
  }

  .btn-outline-custom-primary {
    color: var(--color-primary);
    border-color: var(--color-primary);
    background-color: transparent;
  }

  .btn-outline-custom-primary:hover {
    background-color: var(--color-primary);
    color: #fff;
  }
</style>

