<template>
  <BaseTable
    title="Reports"
    :loading="loading"
    :columns="columns"
    :items="reports"
    :total-elements="totalElements"
    :total-pages="totalPages"
    :current-page="currentPage"
    item-label="reports"
    empty-text="No reports available"
    empty-icon="bi bi-inbox"
    @row-click="navigateToReport"
    @page-change="handlePageChange"
  >
    <template #id="{ item }">
      <i class="bi bi-file-earmark-text icon"></i>
      {{ item.id }}
    </template>
    <template #generatedAt="{ value }">
      {{ formatDateTime(value) }}
    </template>
    <template #status="{ item }">
      {{ item.status }}
    </template>
    <template #epsilonUsed="{ item }">
      {{ calculateEpsilonUsed(item).toFixed(2) }}
    </template>
    <template #totalChecks="{ item }">
      {{ item.results?.length || 0 }}
    </template>
  </BaseTable>
</template>

<script setup>
  import { computed } from 'vue';
  import { useRouter } from 'vue-router';
  import BaseTable from '@/components/BaseTable.vue';

  const router = useRouter();

  defineProps({
    reports: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    totalElements: {
      type: Number,
      default: 0,
    },
    totalPages: {
      type: Number,
      default: 1,
    },
    currentPage: {
      type: Number,
      default: 0,
    },
  });

  const emit = defineEmits(['page-change']);

  const handlePageChange = (page) => {
    emit('page-change', page);
  };

  const columns = computed(() => [
    { key: 'id', label: 'Report ID' },
    { key: 'generatedAt', label: 'Generated At' },
    { key: 'status', label: 'Status', headerClass: 'center', cellClass: getStatusCellClass },
    {
      key: 'numberOfEntities',
      label: 'Entities',
      headerClass: 'center hide-sm',
      cellClass: 'center hide-sm',
      fallback: 'N/A',
    },
    {
      key: 'epsilonUsed',
      label: 'Used',
      headerClass: 'center hide-sm',
      cellClass: getEpsilonCellClass,
    },
    {
      key: 'totalChecks',
      label: 'Checks',
      headerClass: 'center hide-md',
      cellClass: 'center hide-md',
    },
  ]);

  const navigateToReport = (item) => {
    router.push(`/reports/${item.id}`);
  };

  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const calculateEpsilonUsed = (report) => {
    if (!report.results || !Array.isArray(report.results)) return 0;
    return report.results.reduce((sum, result) => sum + (result.epsilon || 0), 0);
  };

  const getStatusCellClass = (item) => {
    const base = 'center';
    switch (item.status) {
      case 'COMPLETED':
        return `${base} success`;
      case 'GENERATING':
        return `${base} warning`;
      case 'FAILED':
        return `${base} danger`;
      default:
        return base;
    }
  };

  const getEpsilonCellClass = (item) => {
    const used = calculateEpsilonUsed(item);
    return used > 3.0 ? 'center hide-sm danger' : 'center hide-sm';
  };
</script>

<style scoped>
  .icon {
    color: var(--color-primary);
    margin-right: var(--spacing-sm);
  }
</style>
