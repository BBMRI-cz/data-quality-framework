<template>
  <div class="container-fluid py-3 py-md-4">
    <!-- Agent Header -->
    <div class="row mb-4">
      <div class="col-12">
        <PageHeader
          :title="agentName"
          :subtitle="`Agent ID: ${agentId}`"
          icon="bi bi-file-earmark-text-fill"
          :editable="true"
          @update:title="handleUpdateAgentName"
        />
      </div>
    </div>

    <!-- Agent Actions -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="d-flex gap-2">
          <button
            class="btn btn-outline-secondary btn-sm d-flex align-items-center"
            @click="goBack"
          >
            <i class="bi bi-arrow-left me-2"></i>
            Back to Agents
          </button>
          <button
            class="btn btn-outline-primary btn-sm d-flex align-items-center"
            @click="goToInteractions"
          >
            <i class="bi bi-clock-history me-2"></i>
            View Logs
          </button>
          <button
            class="btn btn-outline-danger btn-sm d-flex align-items-center"
            :disabled="processing"
            @click="confirmDeleteAgent"
          >
            <i class="bi bi-trash me-2"></i>
            Delete Agent
          </button>
        </div>
      </div>
    </div>

    <!-- Pending Agent Banner -->
    <div v-if="agent && agent.status === 'PENDING'" class="row mb-4">
      <div class="col-12">
        <div
          class="alert alert-warning d-flex align-items-center justify-content-between"
          role="alert"
        >
          <div class="d-flex align-items-center">
            <i class="bi bi-exclamation-triangle me-3" style="font-size: 1.25rem"></i>
            <div>
              <strong>Requires Attention</strong>
              <p class="mb-0 small mt-1">This agent is awaiting approval to join the network</p>
            </div>
          </div>
          <div class="d-flex gap-2 ms-3">
            <button
              class="btn btn-success btn-sm d-flex align-items-center"
              :disabled="processing"
              @click.stop="approveAgent(agent)"
            >
              <i class="bi bi-check-lg me-1"></i>
              Approve
            </button>
            <button
              class="btn btn-danger btn-sm d-flex align-items-center"
              :disabled="processing"
              @click.stop="declineAgent(agent)"
            >
              <i class="bi bi-x-lg me-1"></i>
              Decline
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Agent Report Content -->
    <div>
      <!-- Stats Cards -->
      <div class="row mb-4">
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Total Reports"
            :value="totalReports"
            color="var(--color-primary)"
            icon="bi bi-file-text"
            trend-text="All time"
            trend-type="neutral"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Failed Checks"
            :value="reportStats.failed"
            color="var(--color-danger)"
            icon="bi bi-x-circle"
            trend-text="Needs attention"
            trend-type="negative"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Warnings"
            :value="reportStats.warnings"
            color="var(--color-warning)"
            icon="bi bi-exclamation-triangle"
            trend-text="Review recommended"
            trend-type="neutral"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Last Report"
            :value="reportStats.lastReportTime"
            color="var(--color-primary-dark)"
            icon="bi bi-clock"
            trend-text="Timestamp"
            trend-type="neutral"
          />
        </div>
      </div>

      <div class="row mb-4">
        <div class="col-12 col-md-6 mb-3">
          <StatsCard
            label="Agent Version"
            :value="agentVersion"
            color="var(--color-gray-600)"
            trend-text="Reported version"
            trend-type="neutral"
          />
        </div>
        <div class="col-12 col-md-6 mb-3">
          <StatsCard
            label="Agent Status"
            :value="agentStatusLabel"
            :color="agentStatusColor"
            icon="bi bi-activity"
            trend-text="Current state"
            :trend-type="agentStatusTrendType"
          />
        </div>
      </div>

      <!-- Recent Reports Table -->
      <div class="row">
        <div class="col-12">
          <PaginatedTable
            title="Recent Reports"
            :columns="columns"
            :items="tableRows"
            :page="currentPage"
            :page-size="pageSize"
            :total-items="totalReports"
            :loading="loading"
            :error="error"
            loading-text="Loading agent report..."
            error-title="Unable to load agent report"
            item-key="id"
            item-label="reports"
            empty-text="No reports available"
            @row-click="openReport"
            @page-change="changePage"
          >
            <template #header-meta>
              <Badge :text="`${totalReports} reports`" variant="secondary" size="small" />
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

    <!-- Delete Confirmation Modal -->
    <BaseModal
      :show="showDeleteModal"
      title="Delete Agent"
      subtitle="This action cannot be undone"
      icon="bi bi-trash"
      variant="danger"
      size="sm"
      :loading="processing"
      :save-button-props="{ text: 'Delete', variant: 'danger' }"
      :cancel-button-props="{ text: 'Cancel' }"
      @close="closeDeleteModal"
      @save="deleteAgent"
    >
      <p class="mb-3">
        Are you sure you want to delete <strong>{{ agentName }}</strong
        >?
      </p>
      <p class="mb-0 text-muted small">This will permanently remove:</p>
      <ul class="text-muted small mb-0">
        <li>The agent and all its configuration</li>
        <li>
          All associated reports ({{ totalReports }} report{{ totalReports !== 1 ? 's' : '' }})
        </li>
        <li>All quality check results</li>
        <li>All interaction history</li>
      </ul>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref, onMounted, computed } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import Badge from '@/components/ui/Badge.vue';
  import { useReportTableRows } from '@/composables/useReportTableRows.js';
  import { useAgentReportData } from '@/composables/useAgentReportData.js';
  import { useAgentReportStats } from '@/composables/useAgentReportStats.js';
  import { useAgentManagementActions } from '@/composables/useAgentManagementActions.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import BaseModal from '@/components/BaseModal.vue';

  const route = useRoute();
  const router = useRouter();

  const agentId = ref(route.params.uuid);
  const {
    loading,
    error,
    agent,
    reports,
    qualityChecks,
    currentPage,
    pageSize,
    totalReports,
    latestReport,
    fetchAgentDetails,
    changePage,
  } = useAgentReportData(agentId);

  const {
    qualityCheckMap,
    reportStats,
    agentName,
    agentVersion,
    agentStatusLabel,
    agentStatusColor,
    agentStatusTrendType,
  } = useAgentReportStats({
    agent,
    latestReport,
    qualityChecks,
    totalReports,
  });

  const {
    processing,
    showDeleteModal,
    approveAgent,
    declineAgent,
    handleUpdateAgentName,
    confirmDeleteAgent,
    closeDeleteModal,
    deleteAgent,
  } = useAgentManagementActions({
    agent,
    agentId,
    error,
    onDeleted: () => router.push({ name: 'Agents' }),
  });

  const agentArray = computed(() => {
    return agent.value ? [agent.value] : [];
  });

  const { columns, tableRows } = useReportTableRows({
    reports,
    qualityCheckMap,
    agents: agentArray,
  });

  const openReport = (report) => {
    router.push({ name: 'ReportDetail', params: { id: report.id } });
  };

  const goToInteractions = () => {
    router.push({ name: 'AgentInteractions', params: { uuid: agentId.value } });
  };

  const goBack = () => {
    router.go(-1);
  };

  onMounted(() => {
    fetchAgentDetails();
  });
</script>
