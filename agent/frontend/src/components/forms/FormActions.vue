<template>
  <div class="form-actions" :class="{ 'form-actions-stacked': stacked }">
    <div v-if="$slots.left || showDeleteButton" class="form-actions-left">
      <slot name="left">
        <button
          v-if="showDeleteButton"
          type="button"
          class="btn btn-danger"
          :disabled="loading"
          @click="$emit('delete')"
        >
          <i class="bi bi-trash me-2"></i>
          {{ deleteText }}
        </button>
      </slot>
    </div>

    <div class="form-actions-right">
      <slot name="right">
        <CancelButton
          v-if="showCancelButton"
          :disabled="loading"
          :icon="cancelIcon"
          :text="cancelText"
          @click="$emit('cancel')"
        />
        <SaveButton
          v-if="showSaveButton"
          :type="submitType"
          :loading="loading"
          :disabled="saveDisabled"
          :icon="saveIcon"
          :text="saveText"
          @click="$emit('save')"
        />
      </slot>
    </div>
  </div>
</template>

<script setup>
  import CancelButton from '@/components/CancelButton.vue';
  import SaveButton from '@/components/SaveButton.vue';

  defineProps({
    loading: {
      type: Boolean,
      default: false,
    },
    saveDisabled: {
      type: Boolean,
      default: false,
    },
    showCancelButton: {
      type: Boolean,
      default: true,
    },
    showSaveButton: {
      type: Boolean,
      default: true,
    },
    showDeleteButton: {
      type: Boolean,
      default: false,
    },
    cancelText: {
      type: String,
      default: 'Cancel',
    },
    saveText: {
      type: String,
      default: 'Save',
    },
    deleteText: {
      type: String,
      default: 'Delete',
    },
    cancelIcon: {
      type: String,
      default: 'bi-arrow-left',
    },
    saveIcon: {
      type: String,
      default: 'bi-check-circle',
    },
    submitType: {
      type: String,
      default: 'submit',
    },
    stacked: {
      type: Boolean,
      default: false,
    },
  });

  defineEmits(['cancel', 'save', 'delete']);
</script>

<style scoped>
  .form-actions {
    display: flex;
    gap: var(--spacing-md);
    padding: var(--spacing-2xl);
    border-top: 1px solid var(--color-gray-200);
    background-color: var(--color-gray-50);
    border-radius: 0 0 var(--radius-md) var(--radius-md);
    justify-content: center;
    align-items: center;
  }

  .form-actions-left {
    margin-right: auto;
  }

  .form-actions-right {
    display: flex;
    gap: var(--spacing-md);
    justify-content: center;
  }

  .form-actions :deep(.btn) {
    min-width: 120px;
  }

  .btn-danger {
    background: var(--color-danger);
    color: white;
    border: none;
    padding: var(--spacing-sm) var(--spacing-lg);
    border-radius: var(--radius-sm);
    font-weight: 600;
    transition: all var(--transition-base);
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .btn-danger:hover:not(:disabled) {
    background: #b91c1c;
    transform: translateY(-1px);
  }

  .btn-danger:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  /* Responsive adjustments */
  @media (max-width: 768px) {
    .form-actions {
      flex-direction: column;
      padding: var(--spacing-lg);
    }

    .form-actions-left {
      margin-right: 0;
      width: 100%;
    }

    .form-actions-right {
      flex-direction: column;
      width: 100%;
    }

    .form-actions :deep(.btn) {
      width: 100%;
    }
  }

  .form-actions-stacked {
    flex-direction: column;
  }

  .form-actions-stacked .form-actions-left {
    margin-right: 0;
    width: 100%;
  }

  .form-actions-stacked .form-actions-right {
    flex-direction: column;
    width: 100%;
  }
</style>

