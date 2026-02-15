<template>
  <div class="form-field" :class="{ 'has-error': hasError }">
    <label v-if="label" :for="inputId" class="form-label">
      <i v-if="icon" :class="['bi', icon]"></i>
      <span>{{ label }}</span>
      <span v-if="required" class="required-indicator">*</span>
    </label>

    <div class="input-wrapper">
      <slot>
        <input
          :id="inputId"
          ref="inputRef"
          v-model="modelValue"
          :type="type"
          class="form-control"
          :class="{ 'is-invalid': hasError }"
          :placeholder="placeholder"
          :required="required"
          :disabled="disabled"
          :readonly="readonly"
          :maxlength="maxlength"
          :min="min"
          :max="max"
          :step="step"
          :pattern="pattern"
          v-bind="$attrs"
        />
      </slot>
    </div>

    <div v-if="hasError" class="invalid-feedback">
      {{ errorMessage }}
    </div>

    <small v-if="helpText && !hasError" class="form-help">
      <i v-if="helpIcon" :class="['bi', helpIcon, 'me-1']"></i>
      {{ helpText }}
    </small>
  </div>
</template>

<script setup>
  import { computed, ref } from 'vue';

  const props = defineProps({
    modelValue: {
      type: [String, Number],
      default: '',
    },
    label: {
      type: String,
      default: '',
    },
    icon: {
      type: String,
      default: '',
    },
    type: {
      type: String,
      default: 'text',
    },
    placeholder: {
      type: String,
      default: '',
    },
    helpText: {
      type: String,
      default: '',
    },
    helpIcon: {
      type: String,
      default: '',
    },
    error: {
      type: String,
      default: '',
    },
    required: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
    maxlength: {
      type: [String, Number],
      default: undefined,
    },
    min: {
      type: [String, Number],
      default: undefined,
    },
    max: {
      type: [String, Number],
      default: undefined,
    },
    step: {
      type: [String, Number],
      default: undefined,
    },
    pattern: {
      type: String,
      default: undefined,
    },
    id: {
      type: String,
      default: '',
    },
  });

  const emit = defineEmits(['update:modelValue']);

  const inputRef = ref(null);

  const inputId = computed(
    () => props.id || `form-field-${Math.random().toString(36).substr(2, 9)}`
  );

  const modelValue = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value),
  });

  const hasError = computed(() => !!props.error);
  const errorMessage = computed(() => props.error);

  defineOptions({
    inheritAttrs: false,
  });

  defineExpose({
    focus: () => inputRef.value?.focus(),
    inputRef,
  });
</script>

<style scoped>
  .form-field {
    margin-bottom: var(--spacing-xl);
  }

  .form-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--color-gray-700);
    margin-bottom: var(--spacing-sm);
  }

  .form-label i {
    color: var(--color-primary);
    font-size: 1rem;
  }

  .required-indicator {
    color: var(--color-danger);
    font-weight: 700;
  }

  .form-control {
    width: 100%;
    padding: 0.75rem var(--spacing-md);
    font-size: 1rem;
    border: 2px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    transition: all var(--transition-base);
    background: var(--bg-card);
  }

  .form-control:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  }

  .form-control:disabled {
    background-color: var(--color-gray-100);
    cursor: not-allowed;
  }

  .form-control.is-invalid {
    border-color: var(--color-danger);
    background-color: #fff5f5;
  }

  .form-control.is-invalid:focus {
    box-shadow: 0 0 0 4px rgba(220, 53, 69, 0.1);
  }

  .invalid-feedback {
    display: block;
    margin-top: var(--spacing-xs);
    font-size: 0.875rem;
    color: var(--color-danger);
  }

  .form-help {
    display: block;
    margin-top: var(--spacing-sm);
    font-size: 0.875rem;
    color: var(--color-gray-500);
    line-height: 1.4;
  }

  .form-help i {
    color: var(--color-gray-400);
  }
</style>
