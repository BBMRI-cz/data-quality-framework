<template>
  <BaseModal
    :show="show"
    :title="title"
    :subtitle="subtitle"
    icon="bi bi-exclamation-triangle-fill"
    header-class="bg-danger text-white"
    size="md"
    @close="handleCancel"
  >
    <template #default>
      <div class="text-center py-3">
        <p class="mb-0 fs-5">{{ message }}</p>
        <p v-if="warning" class="text-danger mt-2 small fw-bold">
          <i class="bi bi-exclamation-circle me-1"></i>
          {{ warning }}
        </p>
      </div>
    </template>

    <template #footer>
      <div class="d-flex gap-2 justify-content-end w-100">
        <button
          type="button"
          class="btn btn-light"
          @click="handleCancel"
          :disabled="loading"
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn btn-danger d-flex align-items-center gap-2"
          @click="handleConfirm"
          :disabled="loading"
        >
          <span v-if="loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
          <i v-else class="bi bi-trash"></i>
          {{ confirmText }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup>
import BaseModal from './BaseModal.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: 'Confirm Delete'
  },
  subtitle: {
    type: String,
    default: ''
  },
  message: {
    type: String,
    required: true
  },
  warning: {
    type: String,
    default: ''
  },
  confirmText: {
    type: String,
    default: 'Delete'
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'confirm'])

const handleCancel = () => {
  if (!props.loading) {
    emit('close')
  }
}

const handleConfirm = () => {
  emit('confirm')
}
</script>

