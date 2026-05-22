<template>
  <FormSelect
    id="databaseType"
    v-model="modelValue"
    label="Database Type"
    icon="bi-diagram-3"
    :options="options"
    help-text="Select the data source used by this agent"
    :required="required"
  />
</template>

<script setup>
  import { computed } from 'vue';
  import { FormSelect } from '@/components/forms';

  const props = defineProps({
    modelValue: {
      type: String,
      required: true,
    },
    options: {
      type: Array,
      required: true,
    },
    required: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['update:modelValue']);

  const modelValue = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value),
  });
</script>
