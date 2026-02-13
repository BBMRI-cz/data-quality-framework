<template>
  <div class="quality-check-page">
    <PageHeader
      :title="isEditing ? 'Edit Quality Check' : 'Add Quality Check'"
      :mobile-title="isEditing ? 'Edit Check' : 'Add Check'"
      :subtitle="isEditing ? 'Update check configuration' : 'Create a new quality check'"
      icon="bi bi-check2-square"
    />

    <div class="page-content">
      <div class="form-container">
        <form class="quality-check-form" @submit.prevent="onSave">
          <QualityCheckBasicInfo
            v-model:name="formData.name"
            v-model:description="formData.description"
            :errors="errors"
          />

          <QualityCheckQueryConfig v-model:query="formData.query" :errors="errors" />

          <QualityCheckThresholds
            v-model:warning-threshold="formData.warningThreshold"
            v-model:error-threshold="formData.errorThreshold"
            v-model:epsilon-budget="formData.epsilonBudget"
          />

          <!-- Form Actions -->
          <div class="form-actions">
            <button
              v-if="isEditing"
              type="button"
              class="btn btn-danger"
              :disabled="saving"
              @click="onDelete"
            >
              <i class="bi bi-trash me-2"></i>
              Delete
            </button>
            <div class="form-actions-right">
              <button type="button" class="btn btn-secondary" @click="goBack">
                <i class="bi bi-arrow-left me-2"></i>
                Cancel
              </button>
              <SaveButton
                type="submit"
                :loading="saving"
                :text="
                  saving
                    ? isEditing
                      ? 'Updating...'
                      : 'Creating...'
                    : isEditing
                      ? 'Update Check'
                      : 'Create Check'
                "
              />
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import SaveButton from '@/components/SaveButton.vue';
  import { useQualityCheckForm } from '@/composables/useQualityCheckForm.js';
  import QualityCheckBasicInfo from '@/components/quality-checks/QualityCheckBasicInfo.vue';
  import QualityCheckQueryConfig from '@/components/quality-checks/QualityCheckQueryConfig.vue';
  import QualityCheckThresholds from '@/components/quality-checks/QualityCheckThresholds.vue';

  const route = useRoute();
  const router = useRouter();

  const { formData, errors, saving, isEditing, loadCheck, saveCheck, deleteCheck } =
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
    padding: 2rem;
  }

  .page-content {
    width: 100%;
  }

  .form-container {
    max-width: 900px;
    margin: 0 auto;
  }

  .quality-check-form {
    background: white;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  /* Form Actions */
  .form-actions {
    display: flex;
    gap: 1rem;
    padding: 2rem;
    border-top: 1px solid #e9ecef;
    background-color: #f8f9fa;
    border-radius: 0 0 0.5rem 0.5rem;
    justify-content: space-between;
    align-items: center;
  }

  .form-actions-right {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
  }

  .form-actions .btn {
    min-width: 120px;
  }

  /* Responsive adjustments */
  @media (max-width: 768px) {
    .quality-check-page {
      padding: 1rem;
    }

    .form-actions {
      flex-direction: column;
      padding: 1.5rem;
    }

    .form-actions-right {
      flex-direction: column;
      width: 100%;
    }

    .form-actions .btn {
      width: 100%;
    }
  }

  @media (max-width: 576px) {
    .quality-check-page {
      padding: 0.75rem;
    }

    .form-actions {
      padding: 1rem;
    }
  }
</style>
