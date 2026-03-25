<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          title="Reports"
          mobile-title="Reports"
          subtitle="View and analyze data quality reports from all agents"
          icon="bi bi-file-earmark-text"
        >
          <template #actions>
            <button class="btn btn-outline-primary btn-sm" :disabled="loading" @click="fetchData">
              <i class="bi bi-arrow-clockwise"></i>
              <span class="d-none d-md-inline ms-1">Refresh</span>
            </button>
          </template>
        </PageHeader>

        <!-- Stats Cards -->
        <div class="row g-3 mb-3 mb-md-4">
          <div class="col-12 col-sm-6 col-lg-3">
            <StatsCard
              label="Total Reports"
              :value="reports.length"
              icon="bi bi-file-earmark-text"
              color="var(--color-primary)"
            />
          </div>
          <div class="col-12 col-sm-6 col-lg-3">
            <StatsCard
              label="Passed"
              :value="reportStats.passed"
              :icon="passedStatus.icon"
              :color="passedStatus.color"
            />
          </div>
          <div class="col-12 col-sm-6 col-lg-3">
            <StatsCard
              label="With Warnings"
              :value="reportStats.warnings"
              :icon="warningStatus.icon"
              :color="warningStatus.color"
            />
          </div>
          <div class="col-12 col-sm-6 col-lg-3">
            <StatsCard
              label="Failed"
              :value="reportStats.failed"
              :icon="failedStatus.icon"
              :color="failedStatus.color"
            />
          </div>
        </div>

        <!-- Reports table -->
        <div>
          <div class="mb-4">
            <LabeledValuesFilter v-model="selectedStatus" label="Status:" :categories="statuses" />
          </div>
          <PaginatedTable
            title="Recent Reports"
            :columns="columns"
            :items="tableRows"
            :total-items="filteredReports.length"
            :loading="loading"
            :error="error"
            :paginate="false"
            item-key="id"
            item-label="reports"
            empty-text="No reports available"
            @row-click="openReport"
          >
            <template #header-meta>
              <Badge :text="`${filteredReports.length} reports`" variant="secondary" size="small" />
            </template>

            <template #cell-status="{ item, value }">
              <div class="d-flex align-items-center gap-1">
                <Badge :text="value" :color="item.statusColor" size="small" />
              </div>
            </template>

            <template #cell-warnings="{ value }">
              <span :class="value > 0 ? 'text-warning fw-semibold' : 'text-muted'">{{
                value
              }}</span>
            </template>

            <template #cell-errors="{ value }">
              <span :class="value > 0 ? 'text-danger fw-semibold' : 'text-muted'">{{ value }}</span>
            </template>
          </PaginatedTable>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { useReportsOverview } from '@/composables/useReportsOverview.js';
  import { useReportTableRows } from '@/composables/useReportTableRows.js';
  import { useStatuses } from '@/composables/useStatuses.js';
  import { CheckStatus } from '@/utils/qualityCheckUtils.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import LabeledValuesFilter from '@/components/ui/LabeledValuesFilter.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import Badge from '@/components/ui/Badge.vue';

  const router = useRouter();
  const { getStatusMeta } = useStatuses();
  const passedStatus = getStatusMeta(CheckStatus.PASSED);
  const warningStatus = getStatusMeta(CheckStatus.WARNING);
  const failedStatus = getStatusMeta(CheckStatus.FAILED);

  const {
    reports,
    qualityCheckMap,
    agents,
    loading,
    error,
    selectedStatus,
    statuses,
    reportStats,
    fetchData,
  } = useReportsOverview();

  const { columns, filteredReports, tableRows } = useReportTableRows({
    reports,
    qualityCheckMap,
    agents,
    selectedStatus,
  });

  const openReport = (report) => {
    router.push(`/reports/${report.id}`);
  };

  onMounted(fetchData);
</script>
