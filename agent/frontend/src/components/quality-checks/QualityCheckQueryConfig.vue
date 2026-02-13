<template>
  <div class="form-section">
    <h6 class="section-title">
      <i class="bi bi-code-square me-2"></i>
      Query Configuration
    </h6>
    <div class="mb-3">
      <label for="checkQuery" class="form-label fw-semibold">
        Query <span class="text-danger">*</span>
      </label>
      <textarea
        id="checkQuery"
        :value="query"
        class="form-control font-monospace query-input"
        rows="8"
        placeholder="Enter your CQL query here..."
        :class="{ 'is-invalid': errors.query }"
        required
        @input="$emit('update:query', $event.target.value)"
      ></textarea>
      <div v-if="errors.query" class="invalid-feedback">
        {{ errors.query }}
      </div>
      <small class="form-text text-muted">
        <i class="bi bi-lightbulb me-1"></i>
        Write a CQL query to validate data quality
      </small>
    </div>
  </div>
</template>

<script setup>
  defineProps({
    query: {
      type: String,
      default: '',
    },
    errors: {
      type: Object,
      default: () => ({}),
    },
  });

  defineEmits(['update:query']);
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

  .form-text {
    display: block;
    margin-top: 0.25rem;
    font-size: 0.813rem;
  }

  .query-input {
    background-color: #f8f9fa;
    line-height: 1.6;
  }

  .font-monospace {
    font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
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
