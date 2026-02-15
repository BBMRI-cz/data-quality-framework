<template>
  <FormSection title="Thresholds & Budget" icon="bi-sliders">
    <FormRow :cols="3">
      <FormField
        id="checkWarningThreshold"
        v-model="warningThreshold"
        type="number"
        label="Warning Threshold"
        icon="bi-exclamation-triangle"
        placeholder="10"
        help-text="Trigger warning at this value"
      />

      <FormField
        id="checkErrorThreshold"
        v-model="errorThreshold"
        type="number"
        label="Error Threshold"
        icon="bi-x-circle"
        placeholder="30"
        help-text="Trigger error at this value"
      />

      <FormField
        id="checkEpsilonBudget"
        v-model="epsilonBudget"
        type="number"
        step="0.1"
        label="Epsilon Budget"
        icon="bi-shield-check"
        placeholder="1.0"
        help-text="Privacy budget allocation"
      />
    </FormRow>
  </FormSection>
</template>

<script setup>
  import { FormSection, FormField, FormRow } from '@/components/forms';

  const warningThreshold = defineModel('warningThreshold', { type: Number, default: 10 });
  const errorThreshold = defineModel('errorThreshold', { type: Number, default: 30 });
  const epsilonBudget = defineModel('epsilonBudget', { type: Number, default: 1.0 });
</script>

<style scoped>
  :deep(.form-row) {
    margin-bottom: 0;
  }

  :deep(.form-field) {
    margin-bottom: 0;
  }
</style>
