<template>
  <FormSection title="Query Configuration" icon="bi-code-square">
    <FormTextarea
      id="checkQuery"
      :model-value="query"
      label="Query"
      icon="bi-terminal"
      :rows="8"
      placeholder="Enter your CQL query here..."
      help-text="Write a CQL query to validate data quality"
      help-icon="bi-lightbulb"
      :error="errors.query"
      required
      monospace
      @update:model-value="$emit('update:query', $event)"
    />
  </FormSection>
</template>

<script setup>
  import { FormSection, FormTextarea } from '@/components/forms';

  defineProps({
    query: {
      type: String,
      default: '',
    },
    errors: {
      type: Object,
      default: () => ({}),
    },
  });

  defineEmits(['update:query']);
</script>
