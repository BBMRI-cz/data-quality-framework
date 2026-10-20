<template>
  <FormField
    id="omicsboxUrl"
    v-model="settings.url"
    type="url"
    label="OmicsBox URL"
    icon="bi-link-45deg"
    placeholder="http://omicsbox:8080"
    help-text="Base URL of the OmicsBox sandbox HTTP API"
    :required="required"
  />

  <FormField
    id="omicsboxApiToken"
    v-model="settings.apiToken"
    type="password"
    label="OmicsBox API Token"
    icon="bi-key"
    placeholder="Enter API token"
    help-text="Bearer token the sandbox was started with (OMICSBOX_API_TOKEN). Leave empty if it runs without authentication"
    autocomplete="off"
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
