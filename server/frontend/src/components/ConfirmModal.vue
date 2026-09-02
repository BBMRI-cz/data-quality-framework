<template>
  <BaseModal
    :show="show"
    :title="title"
    :subtitle="subtitle"
    :icon="icon"
    header-class="bg-success text-white"
    size="md"
    @close="handleCancel"
  >
    <template #default>
      <div class="text-center py-3">
        <p class="mb-0 fs-5">{{ message }}</p>
        <slot name="body" />
      </div>
    </template>

    <template #footer>
      <div class="d-flex gap-2 justify-content-center w-100">
        <button
          type="button"
          class="btn btn-danger d-flex align-items-center gap-2"
          :disabled="loading"
          @click="handleCancel"
        >
          <i v-if="cancelIcon" :class="['bi', cancelIcon]"></i>
          {{ cancelText }}
        </button>
        <button
          type="button"
          class="btn btn-success d-flex align-items-center gap-2"
          :disabled="loading"
          @click="handleConfirm"
        >
          <span
            v-if="loading"
            class="spinner-border spinner-border-sm"
            role="status"
            aria-hidden="true"
          ></span>
          <i v-else-if="confirmIcon" :class="['bi', confirmIcon]"></i>
          {{ confirmText }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup>
  import BaseModal from './BaseModal.vue';

  const props = defineProps({
    show: {
      type: Boolean,
      default: false,
    },
    title: {
      type: String,
      default: 'Confirm',
    },
    subtitle: {
      type: String,
      default: '',
    },
    message: {
      type: String,
      required: true,
    },
    icon: {
      type: String,
      default: 'bi-question-circle-fill',
    },
    confirmText: {
      type: String,
      default: 'Confirm',
    },
    confirmIcon: {
      type: String,
      default: 'bi-check-lg',
    },
    cancelText: {
      type: String,
      default: 'Cancel',
    },
    cancelIcon: {
      type: String,
      default: 'bi-x-lg',
    },
    loading: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['close', 'confirm']);

  const handleCancel = () => {
    if (!props.loading) {
      emit('close');
    }
  };

  const handleConfirm = () => {
    emit('confirm');
  };
</script>
