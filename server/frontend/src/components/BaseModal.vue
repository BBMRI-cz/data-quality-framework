<template>
  <Teleport to="body">
    <!-- Separate backdrop transition -->
    <Transition name="backdrop" appear>
      <div
        v-if="show"
        class="modal-backdrop"
        @click.self="handleBackdropClick"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        :aria-describedby="bodyId"
      >
        <!-- Modal content with its own transition -->
        <Transition name="modal" appear @after-enter="onModalEntered">
          <div
            v-if="show"
            :class="modalDialogClasses"
            role="document"
          >
            <div class="modal-content">
              <!-- Header -->
              <div
                v-if="showHeader"
                :class="headerClasses"
              >
                <slot name="header" :close="handleClose">
                  <div class="modal-header-content">
                    <div v-if="icon" class="modal-icon">
                      <i :class="icon"></i>
                    </div>
                    <div class="modal-title-wrapper">
                      <h5 :id="titleId" class="modal-title">
                        {{ title }}
                      </h5>
                      <small v-if="subtitle" class="modal-subtitle">
                        {{ subtitle }}
                      </small>
                    </div>
                  </div>
                  <button
                    v-if="showCloseButton"
                    type="button"
                    class="modal-close-btn"
                    @click="handleClose"
                    aria-label="Close modal"
                  >
                    <i class="bi bi-x-lg"></i>
                  </button>
                </slot>
              </div>

              <!-- Body -->
              <div
                :id="bodyId"
                :class="bodyClasses"
              >
                <slot :close="handleClose" />
              </div>

              <!-- Footer -->
              <div
                v-if="showFooter"
                :class="footerClasses"
              >
                <slot name="footer" :close="handleClose">
                  <div class="modal-footer-actions">
                    <CancelButton
                      v-if="showCancelButton"
                      @click="handleClose"
                      :disabled="loading"
                      v-bind="cancelButtonProps"
                    />
                    <SaveButton
                      v-if="showSaveButton"
                      @click="handleSave"
                      :loading="loading"
                      :disabled="loading || saveDisabled"
                      v-bind="saveButtonProps"
                    />
                  </div>
                </slot>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue';
import CancelButton from './CancelButton.vue';
import SaveButton from './SaveButton.vue';

const props = defineProps({
  // Core modal props
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },

  // Size and layout
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['xs', 'sm', 'md', 'lg', 'xl', 'fullscreen'].includes(value)
  },
  centered: {
    type: Boolean,
    default: true
  },
  scrollable: {
    type: Boolean,
    default: true
  },

  // Appearance
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'success', 'warning', 'danger', 'info', 'light', 'dark'].includes(value)
  },
  backdrop: {
    type: [Boolean, String],
    default: true,
    validator: (value) => [true, false, 'static'].includes(value)
  },

  // Header options
  showHeader: {
    type: Boolean,
    default: true
  },
  showCloseButton: {
    type: Boolean,
    default: true
  },

  // Footer options
  showFooter: {
    type: Boolean,
    default: true
  },
  showCancelButton: {
    type: Boolean,
    default: true
  },
  showSaveButton: {
    type: Boolean,
    default: true
  },

  // Button props
  cancelButtonProps: {
    type: Object,
    default: () => ({})
  },
  saveButtonProps: {
    type: Object,
    default: () => ({ text: 'Save' })
  },

  // State
  loading: {
    type: Boolean,
    default: false
  },
  saveDisabled: {
    type: Boolean,
    default: false
  },

  // Behavior
  closeOnBackdrop: {
    type: Boolean,
    default: true
  },
  closeOnEscape: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits([
  'close',
  'save',
  'backdrop-click',
  'escape-key'
]);

// Generate unique IDs for accessibility
const titleId = `modal-title-${Math.random().toString(36).substr(2, 9)}`;
const bodyId = `modal-body-${Math.random().toString(36).substr(2, 9)}`;

// Computed classes
const modalDialogClasses = computed(() => [
  'modal-dialog',
  {
    'modal-dialog-centered': props.centered,
    'modal-dialog-scrollable': props.scrollable,
    [`modal-${props.size}`]: props.size !== 'md'
  }
]);

const headerClasses = computed(() => [
  'modal-header',
  `modal-header--${props.variant}`,
  {
    'modal-header--with-icon': props.icon
  }
]);

const bodyClasses = computed(() => [
  'modal-body',
  {
    'modal-body--scrollable': props.scrollable
  }
]);

const footerClasses = computed(() => [
  'modal-footer',
  `modal-footer--${props.variant}`
]);

// Event handlers
const handleClose = () => {
  emit('close');
};

const handleSave = () => {
  emit('save');
};

const handleBackdropClick = () => {
  emit('backdrop-click');

  if (props.closeOnBackdrop && props.backdrop !== 'static') {
    handleClose();
  }
};

const handleEscapeKey = (event) => {
  if (event.key === 'Escape' && props.show) {
    emit('escape-key');

    if (props.closeOnEscape) {
      handleClose();
    }
  }
};

// Modal enter completion handler
const onModalEntered = () => {
  // Modal has finished its enter animation
  // Backdrop transition will have started with delay
};

// Lifecycle
onMounted(() => {
  if (props.closeOnEscape) {
    document.addEventListener('keydown', handleEscapeKey);
  }
});

onUnmounted(() => {
  if (props.closeOnEscape) {
    document.removeEventListener('keydown', handleEscapeKey);
  }
});
</script>

<style scoped>
/* Modal Backdrop */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  padding: 1rem;
}

/* Modal Dialog */
.modal-dialog {
  width: 100%;
  max-width: 500px;
  margin: 0;
  display: flex;
  flex-direction: column;
}

.modal-dialog-centered {
  min-height: calc(100% - 2rem);
  justify-content: center;
}

.modal-dialog-scrollable {
  max-height: calc(100vh - 2rem);
}

/* Modal Sizes */
.modal-xs { max-width: 300px; }
.modal-sm { max-width: 400px; }
.modal-lg { max-width: 800px; }
.modal-xl { max-width: 1200px; }
.modal-fullscreen {
  max-width: none;
  width: calc(100% - 2rem);
  height: calc(100% - 2rem);
}

/* Modal Content */
.modal-content {
  background: white;
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  max-height: 100%;
}

/* Modal Header */
.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.modal-header-content {
  display: flex;
  align-items: center;
  flex: 1;
}

.modal-icon {
  width: 48px;
  height: 48px;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1rem;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.modal-title-wrapper {
  flex: 1;
}

.modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  line-height: 1.4;
  color: inherit;
}

.modal-subtitle {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.875rem;
  opacity: 0.8;
  color: inherit;
}

.modal-close-btn {
  background: none;
  border: none;
  padding: 0.5rem;
  border-radius: 0.375rem;
  transition: all 0.2s ease;
  cursor: pointer;
  margin-left: 1rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.modal-close-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}

/* Header Variants */
.modal-header--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.modal-header--primary .modal-icon {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.modal-header--primary .modal-close-btn {
  color: white;
}

.modal-header--primary .modal-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-header--secondary {
  background: #6c757d;
  color: white;
}

.modal-header--success {
  background: #10b981;
  color: white;
}

.modal-header--warning {
  background: #f59e0b;
  color: white;
}

.modal-header--danger {
  background: #ef4444;
  color: white;
}

.modal-header--info {
  background: #3b82f6;
  color: white;
}

.modal-header--light {
  background: #f8f9fa;
  color: #495057;
}

.modal-header--dark {
  background: #343a40;
  color: white;
}

/* Modal Body */
.modal-body {
  padding: 1.5rem;
  flex: 1;
  color: #374151;
}

.modal-body--scrollable {
  overflow-y: auto;
}

/* Modal Footer */
.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  gap: 0.75rem;
}

.modal-footer-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

/* Backdrop transitions - delayed to appear after modal loads */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* Scrollbar styling */
.modal-body--scrollable::-webkit-scrollbar {
  width: 8px;
}

.modal-body--scrollable::-webkit-scrollbar-track {
  background: #f1f3f4;
}

.modal-body--scrollable::-webkit-scrollbar-thumb {
  background: #c1c7cd;
  border-radius: 4px;
}

.modal-body--scrollable::-webkit-scrollbar-thumb:hover {
  background: #a8b0b8;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .modal-backdrop {
    padding: 0.5rem;
  }

  .modal-dialog {
    max-width: 100%;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 1rem;
  }

  .modal-footer-actions {
    flex-direction: column-reverse;
  }

  .modal-footer-actions > * {
    width: 100%;
  }
}

/* Accessibility improvements */
@media (prefers-reduced-motion: reduce) {
  .modal-enter-active,
  .modal-leave-active {
    /* transition: opacity 0.2s ease; */
  }

  .modal-enter-from,
  .modal-leave-to {
    /* transform: none; */
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .modal-content {
    border: 2px solid;
  }

  .modal-header {
    border-bottom-width: 2px;
  }

  .modal-footer {
    border-top-width: 2px;
  }
}
</style>
