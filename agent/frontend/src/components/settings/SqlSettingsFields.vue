<template>
  <FormField
    id="sqlUrl"
    v-model="settings.url"
    label="SQL JDBC URL"
    icon="bi-link-45deg"
    placeholder="jdbc:postgresql://localhost:5432/quality"
    help-text="JDBC connection string (e.g. Postgres). To query CSV files instead, use jdbc:calcite:directory=/path/to/csvs"
    :required="required"
  />

  <FormField
    id="sqlUsername"
    v-model="settings.username"
    label="SQL Username"
    icon="bi-person"
    placeholder="Enter username"
    help-text="Not required for CSV (Calcite) connections"
    :required="required && !isCalcite"
    autocomplete="username"
  />

  <FormField
    id="sqlPassword"
    v-model="settings.password"
    type="password"
    label="SQL Password"
    icon="bi-key"
    placeholder="Enter password"
    help-text="Not required for CSV (Calcite) connections"
    :required="required && !isCalcite"
    autocomplete="current-password"
  />
</template>

<script setup>
  import { computed, toRef } from 'vue';
  import { FormField } from '@/components/forms';

  const props = defineProps({
    settings: {
      type: Object,
      required: true,
    },
    required: {
      type: Boolean,
      default: false,
    },
  });

  const settings = toRef(props, 'settings');

  const isCalcite = computed(() => (settings.value.url || '').startsWith('jdbc:calcite:'));
</script>
