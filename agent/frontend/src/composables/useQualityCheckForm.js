import { ref, computed } from 'vue';
import { qualityCheckService } from '@/services/qualityCheckService.js';

export function useQualityCheckForm() {
  const formData = ref({
    id: null,
    name: '',
    description: '',
    query: '',
    warningThreshold: 10,
    errorThreshold: 30,
    epsilonBudget: 1.0,
  });

  const errors = ref({});
  const saving = ref(false);
  const loading = ref(false);

  const isEditing = computed(() => !!formData.value.id);

  const validateForm = () => {
    const validationErrors = {};

    if (!formData.value.name?.trim()) {
      validationErrors.name = 'Name is required';
    }

    if (!formData.value.query?.trim()) {
      validationErrors.query = 'CQL query is required';
    }

    errors.value = validationErrors;
    return Object.keys(validationErrors).length === 0;
  };

  const loadCheck = async (id) => {
    if (!id) return;

    loading.value = true;
    try {
      formData.value = await qualityCheckService.get(id);
    } catch (error) {
      console.error('Failed to load check:', error);
      errors.value.general = 'Failed to load check';
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const saveCheck = async () => {
    if (!validateForm()) {
      return false;
    }

    saving.value = true;
    try {
      if (isEditing.value) {
        await qualityCheckService.update(formData.value.id, formData.value);
      } else {
        await qualityCheckService.create(formData.value);
      }
      return true;
    } catch (error) {
      console.error('Failed to save check:', error);
      errors.value.general = 'Failed to save check. Please try again.';
      return false;
    } finally {
      saving.value = false;
    }
  };

  const deleteCheck = async () => {
    if (!formData.value.id) return false;

    saving.value = true;
    try {
      await qualityCheckService.delete(formData.value.id);
      return true;
    } catch (error) {
      console.error('Failed to delete check:', error);
      errors.value.general = 'Failed to delete check. Please try again.';
      return false;
    } finally {
      saving.value = false;
    }
  };

  return {
    formData,
    errors,
    saving,
    loading,
    isEditing,
    loadCheck,
    saveCheck,
    deleteCheck,
    validateForm,
  };
}
