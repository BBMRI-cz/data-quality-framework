<template>
  <FormField
    id="fhirUrl"
    v-model="settings.url"
    type="url"
    label="FHIR® Server URL"
    icon="bi-link-45deg"
    placeholder="https://fhir-server.example.com/fhir"
    help-text="The base URL of your FHIR® server endpoint"
    :required="required"
  />

  <FormField
    id="fhirUsername"
    v-model="settings.username"
    label="FHIR® Username"
    icon="bi-person"
    placeholder="Enter username"
    help-text="Username for authenticating with the FHIR® server"
    :required="required"
    autocomplete="username"
  />

  <FormField
    id="fhirPassword"
    v-model="settings.password"
    type="password"
    label="FHIR® Password"
    icon="bi-key"
    placeholder="Enter password"
    help-text="Password for authenticating with the FHIR® server"
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
