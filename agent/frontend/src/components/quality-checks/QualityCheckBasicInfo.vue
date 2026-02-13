<template>
  <div class="form-section">
    <h6 class="section-title">
      <i class="bi bi-info-circle me-2"></i>
      Basic Information
    </h6>
    <div class="mb-3">
      <label for="checkName" class="form-label fw-semibold">
        Name <span class="text-danger">*</span>
      </label>
      <input
        id="checkName"
        :value="name"
        type="text"
        class="form-control form-control-lg"
        placeholder="Enter check name"
        :class="{ 'is-invalid': errors.name }"
        required
        @input="$emit('update:name', $event.target.value)"
      />
      <div v-if="errors.name" class="invalid-feedback">
        {{ errors.name }}
      </div>
    </div>
    <div class="mb-3">
      <label for="checkDescription" class="form-label fw-semibold">Description</label>
      <textarea
        id="checkDescription"
        :value="description"
        class="form-control"
        rows="2"
        placeholder="Enter a brief description of what this check validates"
        @input="$emit('update:description', $event.target.value)"
      ></textarea>
      <small class="form-text text-muted"
        >Optional: Helps others understand the purpose of this check</small
      >
    </div>
  </div>
</template>

<script setup>
  defineProps({
    name: {
      type: String,
      default: '',
    },
    description: {
      type: String,
      default: '',
    },
    errors: {
      type: Object,
      default: () => ({}),
    },
  });

  defineEmits(['update:name', 'update:description']);
</script>

<style scoped>
  .form-section {
    padding: 2rem;
    border-bottom: 1px solid #e9ecef;
  }

  .section-title {
    color: #495057;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 600;
    padding-bottom: 1rem;
    margin-bottom: 1.5rem;
    border-bottom: 2px solid #e9ecef;
  }

  .form-label {
    margin-bottom: 0.5rem;
    font-size: 0.875rem;
    color: #495057;
  }

  .form-control {
    font-size: 0.875rem;
    border: 1px solid #dee2e6;
    transition: all 0.2s ease;
  }

  .form-control:focus {
    border-color: #667eea;
    box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
  }

  .form-control-lg {
    font-size: 1rem;
    padding: 0.75rem 1rem;
  }

  .form-text {
    display: block;
    margin-top: 0.25rem;
    font-size: 0.813rem;
  }

  .invalid-feedback {
    display: block;
    width: 100%;
    margin-top: 0.25rem;
    font-size: 0.875rem;
    color: #dc3545;
  }

  .is-invalid {
    border-color: #dc3545 !important;
    background-color: #fff5f5;
  }

  .is-invalid:focus {
    box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25);
  }

  /* Responsive adjustments */
  @media (max-width: 768px) {
    .form-section {
      padding: 1.5rem;
    }
  }

  @media (max-width: 576px) {
    .form-section {
      padding: 1rem;
    }
  }
</style>
