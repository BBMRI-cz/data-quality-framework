<template>
  <input
    :value="modelValue"
    type="text"
    class="form-control"
    :placeholder="placeholder"
    @input="onInput"
  />
</template>

<script setup>
  defineProps({
    modelValue: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: 'Search...',
    },
  });

  const emit = defineEmits(['update:modelValue']);

  const onInput = (event) => {
    emit('update:modelValue', event.target.value);
  };
</script>
