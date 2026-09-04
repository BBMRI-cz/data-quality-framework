<template>
  <BaseModal
    :show="show"
    :title="hasExistingKey ? 'Update Public Key' : 'Add Public Key'"
    subtitle="Used to verify manifests signed by this central server"
    icon="bi bi-key fs-3"
    size="lg"
    variant="primary"
    :loading="loading"
    :save-button-props="{ text: 'Save Public Key', icon: 'bi-key' }"
    @close="handleClose"
    @save="handleSave"
  >
    <form class="modal-form" @submit.prevent="handleSave">
      <FormTextarea
        id="serverPublicKey"
        v-model="draftKey"
        label="Public Key (PEM)"
        icon="bi-key"
        placeholder="-----BEGIN PUBLIC KEY-----"
        help-text="Paste the PEM encoded public key. Leave empty to remove the stored key."
        help-icon="bi-info-circle"
        :rows="8"
        maxlength="2048"
        monospace
      />
    </form>
  </BaseModal>
</template>

<script setup>
  import { ref, computed, watch } from 'vue';
  import BaseModal from './BaseModal.vue';
  import { FormTextarea } from '@/components/forms';

  const props = defineProps({
    show: {
      type: Boolean,
      default: false,
    },
    publicKey: {
      type: String,
      default: '',
    },
    loading: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['close', 'save']);

  const draftKey = ref('');

  const hasExistingKey = computed(() => Boolean(props.publicKey));

  function handleClose() {
    emit('close');
  }

  function handleSave() {
    emit('save', draftKey.value.trim());
  }

  watch(
    () => props.show,
    (visible) => {
      if (visible) {
        draftKey.value = props.publicKey || '';
      }
    }
  );
</script>

<style scoped>
  .modal-form {
    display: flex;
    flex-direction: column;
    gap: 0;
  }
</style>
