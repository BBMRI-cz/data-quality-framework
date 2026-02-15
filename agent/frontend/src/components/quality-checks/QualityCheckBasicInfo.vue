<template>
  <FormSection title="Basic Information" icon="bi-info-circle">
    <FormField
      id="checkName"
      :model-value="name"
      label="Name"
      icon="bi-tag"
      placeholder="Enter check name"
      :error="errors.name"
      required
      @update:model-value="$emit('update:name', $event)"
    />

    <FormTextarea
      id="checkDescription"
      :model-value="description"
      label="Description"
      icon="bi-card-text"
      :rows="2"
      placeholder="Enter a brief description of what this check validates"
      help-text="Optional: Helps others understand the purpose of this check"
      @update:model-value="$emit('update:description', $event)"
    />
  </FormSection>
</template>

<script setup>
  import { FormSection, FormField, FormTextarea } from '@/components/forms';

  defineProps({
    name: {
      type: String,
      default: '',
    },
    description: {
      type: String,
      default: '',
    },
    errors: {
      type: Object,
      default: () => ({}),
    },
  });

  defineEmits(['update:name', 'update:description']);
</script>
