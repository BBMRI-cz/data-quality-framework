import { ref, computed } from 'vue';
import { qualityCheckService } from '@/services/qualityCheckService.js';

/**
 * Composable for managing quality checks list with search, filtering, and pagination
 * @returns {Object} Quality checks state and methods
 */
export function useQualityChecks() {
  const qualityChecks = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');
  const pagination = ref({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });

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

  const totalChecks = computed(() => pagination.value.totalElements);

  const totalEpsilonBudget = computed(() =>
    qualityChecks.value.reduce((sum, check) => sum + (check.epsilonBudget || 0), 0)
  );

  const fetchChecks = async ({ page = 0, size = 10 } = {}) => {
    loading.value = true;
    error.value = null;

    try {
      const result = await qualityCheckService.getAll({ page, size });
      qualityChecks.value = result.items;
      pagination.value = {
        page: result.page.number ?? page,
        size: result.page.size ?? size,
        totalElements: result.page.totalElements ?? 0,
        totalPages: result.page.totalPages ?? 0,
      };
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
    totalChecks,
    totalEpsilonBudget,
    loading,
    error,
    searchQuery,
    pagination,
    fetchChecks,
    clearSearch,
  };
}
