<template>
  <div class="form-section">
    <h6 class="section-title">
      <i class="bi bi-sliders me-2"></i>
      Thresholds & Budget
    </h6>
    <div class="row g-3">
      <div class="col-md-4">
        <label for="checkWarningThreshold" class="form-label fw-semibold">
          <i class="bi bi-exclamation-triangle text-warning me-1"></i>
          Warning Threshold
        </label>
        <input
          id="checkWarningThreshold"
          v-model.number="warningThreshold"
          type="number"
          class="form-control"
          placeholder="10"
        />
        <small class="form-text text-muted">Trigger warning at this value</small>
      </div>
      <div class="col-md-4">
        <label for="checkErrorThreshold" class="form-label fw-semibold">
          <i class="bi bi-x-circle text-danger me-1"></i>
          Error Threshold
        </label>
        <input
          id="checkErrorThreshold"
          v-model.number="errorThreshold"
          type="number"
          class="form-control"
          placeholder="30"
        />
        <small class="form-text text-muted">Trigger error at this value</small>
      </div>
      <div class="col-md-4">
        <label for="checkEpsilonBudget" class="form-label fw-semibold">
          <i class="bi bi-shield-check text-info me-1"></i>
          Epsilon Budget
        </label>
        <input
          id="checkEpsilonBudget"
          v-model.number="epsilonBudget"
          type="number"
          step="0.1"
          class="form-control"
          placeholder="1.0"
        />
        <small class="form-text text-muted">Privacy budget allocation</small>
      </div>
    </div>
  </div>
</template>

<script setup>
  const warningThreshold = defineModel('warningThreshold', { type: Number, default: 10 });
  const errorThreshold = defineModel('errorThreshold', { type: Number, default: 30 });
  const epsilonBudget = defineModel('epsilonBudget', { type: Number, default: 1.0 });
</script>

<style scoped>
  .form-section {
    padding: 2rem;
    border-bottom: 1px solid #e9ecef;
  }

  .form-section:last-of-type {
    border-bottom: none;
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

  @media (max-width: 768px) {
    .form-section {
      padding: 1.5rem;
    }
  }

  @media (max-width: 576px) {
    .form-section {
      padding: 1rem;
    }

    .row.g-3 {
      gap: 1rem !important;
    }

    .col-md-4 {
      width: 100%;
    }
  }
</style>
