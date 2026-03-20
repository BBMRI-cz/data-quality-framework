<template>
  <div class="page-header mb-3 mb-md-4">
    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-start gap-2">
      <div class="header-content">
        <div class="d-flex align-items-center gap-2 mb-1">
          <h1 v-if="!isEditing" class="page-title mb-0">
            <slot name="icon">
              <i v-if="icon" :class="['me-2', 'text-primary', icon]"></i>
            </slot>
            <span :class="{ 'd-none d-sm-inline': mobileTitle }">{{ title }}</span>
            <span v-if="mobileTitle" class="d-inline d-sm-none">{{ mobileTitle }}</span>
          </h1>
          <input
            v-else
            ref="titleInput"
            v-model="editingTitle"
            class="form-control form-control-title-edit"
            :disabled="saving"
            @keyup.enter="confirmEdit"
            @keyup.esc="cancelEdit"
          />

          <!-- Edit/Confirm/Cancel Buttons -->
          <template v-if="editable">
            <button
              v-if="!isEditing"
              type="button"
              class="btn-icon-edit"
              title="Edit name"
              @click="startEdit"
            >
              <i class="bi bi-pencil"></i>
            </button>
            <template v-else>
              <button
                type="button"
                class="btn-icon-action btn-icon-confirm"
                :disabled="saving || !editingTitle.trim()"
                title="Confirm"
                @click="confirmEdit"
              >
                <i class="bi bi-check-lg"></i>
              </button>
              <button
                type="button"
                class="btn-icon-action btn-icon-cancel"
                :disabled="saving"
                title="Cancel"
                @click="cancelEdit"
              >
                <i class="bi bi-x-lg"></i>
              </button>
            </template>
          </template>
        </div>
        <p
          v-if="subtitle"
          class="page-subtitle"
          :class="{ 'd-none d-sm-block': hideSubtitleOnMobile }"
        >
          {{ subtitle }}
        </p>
      </div>
      <div class="header-actions d-flex align-items-center gap-3">
        <slot name="toggle"></slot>
        <slot name="actions"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, nextTick } from 'vue';

  const props = defineProps({
    title: {
      type: String,
      required: true,
    },
    subtitle: {
      type: String,
      default: '',
    },
    icon: {
      type: String,
      default: '',
    },
    mobileTitle: {
      type: String,
      default: '',
    },
    hideSubtitleOnMobile: {
      type: Boolean,
      default: true,
    },
    editable: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['update:title']);

  const isEditing = ref(false);
  const editingTitle = ref('');
  const saving = ref(false);
  const titleInput = ref(null);

  const startEdit = () => {
    isEditing.value = true;
    editingTitle.value = props.title;
    nextTick(() => {
      if (titleInput.value) {
        titleInput.value.focus();
        titleInput.value.select();
      }
    });
  };

  const confirmEdit = () => {
    const newTitle = editingTitle.value.trim();

    if (!newTitle) {
      cancelEdit();
      return;
    }

    if (newTitle !== props.title) {
      emit('update:title', newTitle);
    } else {
      isEditing.value = false;
    }
  };

  const cancelEdit = () => {
    isEditing.value = false;
    editingTitle.value = '';
  };

  // Expose method for parent to reset editing state after save
  const resetEditingState = () => {
    isEditing.value = false;
    editingTitle.value = '';
  };

  defineExpose({ resetEditingState });
</script>

<style scoped>
  .page-header {
    padding-bottom: 1rem;
  }

  .page-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: #2c3e50;
    margin: 0;
  }

  .page-subtitle {
    color: #6c757d;
    font-size: 0.875rem;
    margin: 0;
  }

  .header-actions {
    flex-shrink: 0;
  }

  /* Editable Title Input */
  .form-control-title-edit {
    font-size: 1.5rem;
    font-weight: 700;
    padding: 0.25rem 0.5rem;
    border: 2px solid #dee2e6;
    border-radius: 0.375rem;
    color: #2c3e50;
    max-width: 400px;
  }

  .form-control-title-edit:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
  }

  .form-control-title-edit:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  /* Icon Buttons */
  .btn-icon-edit,
  .btn-icon-action {
    width: 32px;
    height: 32px;
    padding: 0;
    border: none;
    border-radius: 0.375rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 0.875rem;
    flex-shrink: 0;
  }

  .btn-icon-edit {
    background: #f8f9fa;
    color: #6c757d;
  }

  .btn-icon-edit:hover {
    background: #e9ecef;
    color: #495057;
  }

  .btn-icon-confirm {
    background: #28a745;
    color: white;
  }

  .btn-icon-confirm:hover:not(:disabled) {
    background: #218838;
  }

  .btn-icon-cancel {
    background: #dc3545;
    color: white;
  }

  .btn-icon-cancel:hover:not(:disabled) {
    background: #c82333;
  }

  .btn-icon-action:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  /* OIDC Icon */
  :deep(.oidc-icon) {
    margin-right: 0.5rem;
    fill: #0d6efd;
    color: #0d6efd;
    vertical-align: middle;
  }

  @media (min-width: 768px) {
    .page-title {
      font-size: 1.75rem;
    }

    .page-subtitle {
      font-size: 0.95rem;
    }

    .form-control-title-edit {
      font-size: 1.75rem;
    }
  }
</style>
