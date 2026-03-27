import { computed, ref, watch } from 'vue';
import { apiService } from '@/services/apiService.js';
import { formatDateShort } from '@/utils/dateUtils.js';

export function useQualityChecksTable() {
  const qualityChecks = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');
  const selectedCategory = ref(null);
  const currentPage = ref(0);
  const pageSize = 10;

  const categories = computed(() => {
    const names = new Set();

    qualityChecks.value.forEach((check) => {
      const categoryName = check.category?.name || 'No Category';
      names.add(categoryName);
    });

    return Array.from(names).sort();
  });

  const filteredChecks = computed(() => {
    let checks = qualityChecks.value;

    if (selectedCategory.value) {
      checks = checks.filter((check) => {
        const categoryName = check.category?.name || 'No Category';
        return categoryName === selectedCategory.value;
      });
    }

    if (!searchQuery.value) {
      return checks;
    }

    const query = searchQuery.value.toLowerCase();
    return checks.filter(
      (check) =>
        check.name?.toLowerCase().includes(query) ||
        check.description?.toLowerCase().includes(query) ||
        check.hash?.toLowerCase().includes(query) ||
        check.category?.name?.toLowerCase().includes(query)
    );
  });

  const tableRows = computed(() => {
    return filteredChecks.value.map((check) => ({
      ...check,
      description: check.description || 'No description',
      registeredAtText: check.registeredAt ? formatDateShort(check.registeredAt) : null,
    }));
  });

  const emptyStateText = computed(() => {
    return searchQuery.value
      ? 'Try adjusting your search criteria'
      : 'No quality checks are configured yet';
  });

  const onPageChange = (page) => {
    currentPage.value = page;
  };

  const loadQualityChecks = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getQualityChecks();

      if (data._embedded && data._embedded.qualityChecks) {
        qualityChecks.value = data._embedded.qualityChecks;
      } else if (Array.isArray(data)) {
        qualityChecks.value = data;
      } else {
        qualityChecks.value = [];
      }
    } catch (err) {
      error.value = err.message || 'Failed to load quality checks';
      console.error('Error loading quality checks:', err);
    } finally {
      loading.value = false;
    }
  };

  const refreshChecks = () => {
    loadQualityChecks();
  };

  watch([searchQuery, selectedCategory], () => {
    currentPage.value = 0;
  });

  watch(tableRows, (rows) => {
    const maxPage = Math.max(Math.ceil(rows.length / pageSize) - 1, 0);
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage;
    }
  });

  return {
    qualityChecks,
    loading,
    error,
    searchQuery,
    selectedCategory,
    currentPage,
    pageSize,
    categories,
    filteredChecks,
    tableRows,
    emptyStateText,
    onPageChange,
    loadQualityChecks,
    refreshChecks,
  };
}
