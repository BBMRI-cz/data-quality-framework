<template>
  <div class="quality-check-page">
    <PageHeader
      :title="isEditing ? 'Edit Quality Check' : 'Add Quality Check'"
      :mobile-title="isEditing ? 'Edit Check' : 'Add Check'"
      :subtitle="isEditing ? 'Update check configuration' : 'Create a new quality check'"
      icon="bi bi-check2-square"
    />

    <div class="page-content">
      <div class="form-card">
        <div class="section-header">
          <div>
            <h2 class="section-title">
              <i class="bi bi-gear"></i>
              Check Configuration
            </h2>
            <p class="section-description">
              {{
                isEditing
                  ? 'Modify the settings for this quality check'
                  : 'Configure the settings for your new quality check'
              }}
            </p>
          </div>
        </div>

        <form class="check-form" @submit.prevent="onSave">
          <FormField
            id="checkName"
            v-model="formData.name"
            label="Name"
            icon="bi-tag"
            placeholder="Enter check name"
            :error="errors.name"
            required
          />

          <FormTextarea
            id="checkDescription"
            v-model="formData.description"
            label="Description"
            icon="bi-card-text"
            :rows="2"
            placeholder="Enter a brief description of what this check validates"
            help-text="Optional: Helps others understand the purpose of this check"
          />

          <FormSelect
            v-if="!isEditing"
            id="checkType"
            v-model="formData.type"
            label="Check Type"
            icon="bi-diagram-3"
            help-text="CQL checks use Clinical Quality Language queries. Java checks are built-in implementations."
            help-icon="bi-info-circle"
            :options="[
              { value: 'CQL', label: 'CQL (Clinical Quality Language)' },
              { value: 'JAVA', label: 'Java (Built-in Check)' },
            ]"
          />

          <div v-else class="form-field type-display-field">
            <label class="form-label">
              <i class="bi bi-diagram-3"></i>
              <span>Check Type</span>
            </label>
            <div class="type-display">
              <span class="badge" :class="isJavaType ? 'bg-warning text-dark' : 'bg-primary'">
                <i :class="isJavaType ? 'bi bi-code-slash' : 'bi bi-file-code'" class="me-1"></i>
                {{ formData.type }}
              </span>
              <small v-if="isJavaType" class="text-muted ms-2">
                Built-in Java check - query configuration not applicable
              </small>
            </div>
          </div>

          <FormTextarea
            v-if="!isJavaType"
            id="checkQuery"
            v-model="formData.query"
            label="Query"
            icon="bi-terminal"
            :rows="8"
            placeholder="Enter your CQL query here..."
            help-text="Write a CQL query to validate data quality"
            help-icon="bi-lightbulb"
            :error="errors.query"
            required
            monospace
          />

          <FormRow :cols="3">
            <FormField
              id="checkWarningThreshold"
              v-model="formData.warningThreshold"
              type="number"
              label="Warning Threshold"
              icon="bi-exclamation-triangle"
              placeholder="10"
              help-text="Trigger warning at this value"
            />

            <FormField
              id="checkErrorThreshold"
              v-model="formData.errorThreshold"
              type="number"
              label="Error Threshold"
              icon="bi-x-circle"
              placeholder="30"
              help-text="Trigger error at this value"
            />

            <FormField
              id="checkEpsilonBudget"
              v-model="formData.epsilonBudget"
              type="number"
              step="0.1"
              label="Epsilon Budget"
              icon="bi-shield-check"
              placeholder="1.0"
              help-text="Privacy budget allocation"
            />
          </FormRow>

          <FormActions
            :loading="saving"
            :show-delete-button="isEditing && !isJavaType"
            :save-text="isEditing ? 'Update Check' : 'Create Check'"
            :save-icon="isEditing ? 'bi-check-circle' : 'bi-plus-circle'"
            @cancel="goBack"
            @save="onSave"
            @delete="onDelete"
          />
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import { FormField, FormTextarea, FormSelect, FormRow, FormActions } from '@/components/forms';
  import { useQualityCheckForm } from '@/composables/useQualityCheckForm.js';

  const route = useRoute();
  const router = useRouter();

  const { formData, errors, saving, isEditing, isJavaType, loadCheck, saveCheck, deleteCheck } =
    useQualityCheckForm();

  const onSave = async () => {
    const success = await saveCheck();
    if (success) {
      router.push('/quality-checks');
    }
  };

  const goBack = () => {
    router.back();
  };

  const onDelete = async () => {
    if (
      !confirm('Are you sure you want to delete this quality check? This action cannot be undone.')
    ) {
      return;
    }

    const success = await deleteCheck();
    if (success) {
      router.push('/quality-checks');
    }
  };

  onMounted(() => {
    if (route.params.id) {
      loadCheck(route.params.id);
    }
  });
</script>

<style scoped>
  .quality-check-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    max-width: 800px;
    margin: 0 auto;
  }

  .form-card {
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-sm);
    padding: var(--spacing-2xl);
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--spacing-xl);
  }

  .section-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--color-gray-800);
    margin: 0 0 var(--spacing-sm) 0;
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .section-title i {
    color: var(--color-primary);
  }

  .section-description {
    font-size: 0.95rem;
    color: var(--color-gray-500);
    margin: 0;
    line-height: 1.5;
  }

  .check-form {
    max-width: 600px;
  }

  /* Type display field styling */
  .type-display-field {
    margin-bottom: var(--spacing-xl);
  }

  .type-display-field .form-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--color-gray-700);
    margin-bottom: var(--spacing-sm);
  }

  .type-display-field .form-label i {
    color: var(--color-primary);
  }

  .type-display {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }

  .type-display .badge {
    font-size: 0.875rem;
    padding: var(--spacing-sm) 0.75rem;
  }

  /* Override FormRow margin for threshold fields */
  :deep(.form-row .form-field) {
    margin-bottom: 0;
  }

  /* Override FormActions for this context */
  :deep(.form-actions) {
    background-color: transparent;
    border-radius: 0;
    padding: var(--spacing-lg) 0 0 0;
    border-top: 2px solid var(--color-gray-100);
    margin-top: var(--spacing-xl);
    justify-content: flex-start;
  }

  /* Responsive adjustments */
  @media (max-width: 768px) {
    .quality-check-page {
      padding: var(--spacing-md);
    }

    .form-card {
      padding: var(--spacing-lg);
    }

    .section-title {
      font-size: 1.35rem;
    }

    .check-form {
      max-width: 100%;
    }
  }

  @media (max-width: 576px) {
    .quality-check-page {
      padding: 0.75rem;
    }

    .form-card {
      padding: 1.25rem;
    }

    .section-header {
      margin-bottom: var(--spacing-lg);
    }

    .section-title {
      font-size: 1.2rem;
      gap: var(--spacing-sm);
    }
  }
</style>
