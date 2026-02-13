import { ref, computed } from 'vue';
import { qualityCheckService } from '@/services/qualityCheckService.js';

/**
 * Composable for managing quality checks list with search and filtering
 * @returns {Object} Quality checks state and methods
 */
export function useQualityChecks() {
  const qualityChecks = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');

  const filteredChecks = computed(() => {
    if (!searchQuery.value) {
      return qualityChecks.value;
    }

    const query = searchQuery.value.toLowerCase();
    return qualityChecks.value.filter(
      (check) =>
        check.name?.toLowerCase().includes(query) ||
        check.description?.toLowerCase().includes(query) ||
        check.query?.toLowerCase().includes(query)
    );
  });

  const fetchChecks = async () => {
    loading.value = true;
    error.value = null;

    try {
      qualityChecks.value = await qualityCheckService.getAll();
    } catch (err) {
      error.value = err.message || 'Failed to load quality checks';
      console.error('Error fetching quality checks:', err);
      qualityChecks.value = [];
    } finally {
      loading.value = false;
    }
  };

  const clearSearch = () => {
    searchQuery.value = '';
  };

  return {
    qualityChecks,
    filteredChecks,
    loading,
    error,
    searchQuery,
    fetchChecks,
    clearSearch,
  };
}
