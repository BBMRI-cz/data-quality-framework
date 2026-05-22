<template>
  <FormField
    id="sqlUrl"
    v-model="settings.url"
    label="SQL JDBC URL"
    icon="bi-link-45deg"
    placeholder="jdbc:postgresql://localhost:5432/quality"
    help-text="The JDBC connection string for the SQL database"
    :required="required"
  />

  <FormField
    id="sqlUsername"
    v-model="settings.username"
    label="SQL Username"
    icon="bi-person"
    placeholder="Enter username"
    help-text="Username for authenticating with the SQL database"
    :required="required"
    autocomplete="username"
  />

  <FormField
    id="sqlPassword"
    v-model="settings.password"
    type="password"
    label="SQL Password"
    icon="bi-key"
    placeholder="Enter password"
    help-text="Password for authenticating with the SQL database"
    :required="required"
    autocomplete="current-password"
  />
</template>

<script setup>
  import { toRef } from 'vue';
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
</script>
