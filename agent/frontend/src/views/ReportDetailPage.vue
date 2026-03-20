<template>
  <div class="report-detail-page">
    <PageHeader
      :title="`Report ${report?.id || ''}`"
      :mobile-title="`Report ${report?.id || ''}`"
      subtitle="Detailed view of data quality report"
      icon="bi bi-file-earmark-text-fill"
    />

    <div class="page-content">
      <!-- Back button -->
      <div class="page-actions">
        <ActionButton
          to="/reports"
          icon="bi bi-arrow-left"
          text="Back to Reports"
          variant="secondary"
        />
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="text-muted mt-3">Loading report details...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="alert alert-danger">
        <i class="bi bi-exclamation-triangle me-2"></i>
        {{ error }}
      </div>

      <!-- Report details -->
      <template v-else-if="report">
        <!-- Stats Cards -->
        <div class="stats-grid mb-4">
          <StatCard
            :number="report.numberOfEntities?.toLocaleString() || 'N/A'"
            label="Total Entities"
            number-class="text-primary"
          />
          <StatCard
            :number="report.results?.length || 0"
            label="Total Checks"
            number-class="text-secondary"
          />
          <StatCard :number="countErrors()" label="Errors" number-class="text-danger" />
          <StatCard :number="countWarnings()" label="Warnings" number-class="text-warning" />
          <StatCard :number="countPassed()" label="Passed" number-class="text-success" />
        </div>

        <!-- Epsilon Warning Alert -->
        <div v-if="isOverBudget()" class="alert alert-warning mb-4">
          <i class="bi bi-exclamation-triangle me-2"></i>
          <strong>Warning:</strong> Epsilon budget exceeded! Total epsilon used ({{
            calculateEpsilonUsed().toFixed(2)
          }}) exceeds budget ({{ report.epsilonBudget.toFixed(2) }})
        </div>

        <div class="mb-4">
          <div class="filter-label">Status:</div>
          <FilterComponent v-model="selectedStatus" :elements="statuses" />
        </div>

        <!-- Results Section -->
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white border-bottom" style="padding: 0; border: none"></div>
          <div class="card-body">
            <div v-if="filteredResults.length === 0" class="text-center py-4 text-muted">
              <i class="bi bi-inbox fs-1 d-block mb-2 opacity-50"></i>
              <p class="mb-0">No results available</p>
            </div>
            <div v-else class="results-container">
              <div
                v-for="result in filteredResults"
                :id="getCheckIdKey(result)"
                :key="getCheckIdKey(result)"
                :class="['result-card', 'card', 'mb-3', getResultClass(result)]"
              >
                <div class="card-body">
                  <div class="d-flex justify-content-between align-items-start">
                    <div class="flex-grow-1">
                      <button
                        v-if="checkExists(result.checkId)"
                        class="btn btn-link check-link p-0 mb-2"
                        :title="result.checkDescription || 'Click to edit check'"
                        @click="navigateToCheck(result.checkId)"
                      >
                        {{ result.checkName }}
                      </button>
                      <h6 v-else class="card-title mb-2">{{ result.checkName }}</h6>
                      <div v-if="result.checkDescription" class="check-description mb-2">
                        <p class="text-muted small mb-0">{{ result.checkDescription }}</p>
                      </div>
                      <div class="result-details">
                        <div class="detail-row">
                          <span class="detail-label">Occurrence Rate:</span>
                          <span class="detail-value">{{ calculatePercentage(result) }}%</span>
                        </div>
                        <div v-if="result.error" class="detail-row">
                          <span class="detail-label text-danger">Error:</span>
                          <span class="detail-value text-danger">{{ result.error }}</span>
                        </div>
                      </div>
                    </div>
                    <button
                      v-if="Array.isArray(result.patients) && result.patients.length > 0"
                      class="btn btn-sm btn-outline-secondary ms-3"
                      :title="
                        openIds[getCheckIdKey(result)] ? 'Hide Patient IDs' : 'Show Patient IDs'
                      "
                      @click="toggleIds(getCheckIdKey(result))"
                    >
                      <i class="bi bi-person-lines-fill"></i>
                    </button>
                  </div>

                  <!-- Patient IDs Section -->
                  <div
                    v-if="
                      Array.isArray(result.patients) &&
                      result.patients.length > 0 &&
                      openIds[getCheckIdKey(result)]
                    "
                    class="patient-ids-section mt-3"
                  >
                    <hr class="my-3" />
                    <h6 class="mb-2">
                      <i class="bi bi-people-fill me-2"></i>Patient Identifiers ({{
                        result.patients.length
                      }})
                    </h6>
                    <div class="table-responsive">
                      <table class="table table-sm table-hover table-borderless text-center mb-0">
                        <tbody>
                          <tr v-for="(row, rowIndex) in patientTableRows(result)" :key="rowIndex">
                            <td
                              v-for="(patient, colIndex) in row"
                              :key="colIndex"
                              class="patient-cell"
                            >
                              <a
                                v-if="patient"
                                href="#"
                                class="patient-link"
                                @click.prevent="showPatientDetail(patient)"
                              >
                                {{ patient }}
                              </a>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                    <nav
                      v-if="result.patients.length > pageSize"
                      class="d-flex justify-content-center mt-3"
                    >
                      <Pagination
                        :current-page="idPage[getCheckIdKey(result)] || 1"
                        :page-size="pageSize"
                        :total-pages="Math.ceil((result.patients?.length || 0) / pageSize)"
                        :max-visible-buttons="5"
                        @page-changed="(page) => changePage(getCheckIdKey(result), page)"
                      />
                    </nav>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <PatientModal ref="patientModalRef" :patient-id="modalPatientId" />
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, nextTick } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { api } from '@/api';
  import PageHeader from '@/components/PageHeader.vue';
  import PatientModal from '@/components/PatientModal.vue';
  import Pagination from '@/components/Pagination.vue';
  import StatCard from '@/components/StatCard.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import FilterComponent from '@/components/FilterComponent.vue';
  import { useReportStore } from '@/stores/reportStore.js';

  const route = useRoute();
  const router = useRouter();
  const reportStore = useReportStore();

  const loading = ref(true);
  const error = ref(null);
  const report = ref(null);
  const qualityChecks = ref([]);
  const openIds = ref({});
  const pageSize = 60;
  const idPage = ref({});
  const patientModalRef = ref(null);
  const modalPatientId = ref('');
  const selectedStatus = ref(null);

  const CHECK_STATUS = {
    PASSED: 'PASSED',
    WARNING: 'WARNING',
    FAILED: 'FAILED',
  };

  const statuses = [CHECK_STATUS.PASSED, CHECK_STATUS.WARNING, CHECK_STATUS.FAILED];

  const checkExists = (checkId) => {
    return qualityChecks.value.some((check) => check.id === checkId);
  };

  const navigateToCheck = (checkId) => {
    if (checkExists(checkId)) {
      router.push(`/quality-checks/${checkId}/edit`);
    }
  };

  function toggleIds(checkId) {
    openIds.value[checkId] = !openIds.value[checkId];
  }

  function changePage(checkId, page) {
    idPage.value[checkId] = page;
  }

  function paginatedPatients(result) {
    const all = result.patients || [];
    const currentPage = idPage.value[getCheckIdKey(result)] || 1;
    const start = (currentPage - 1) * pageSize;
    return all.slice(start, start + pageSize);
  }

  function patientTableRows(result) {
    const patients = paginatedPatients(result);
    const rows = [];
    for (let i = 0; i < patients.length; i += 6) {
      rows.push(patients.slice(i, i + 6));
    }
    return rows;
  }

  function showPatientDetail(patientId) {
    modalPatientId.value = patientId;
    if (patientModalRef.value) {
      patientModalRef.value.open();
    }
  }

  function getCheckIdKey(result) {
    return result.checkId + '_' + (result.stratum || 'all');
  }

  const getOccurrenceValue = (result) => {
    const rawValue = Number(result?.rawValue);
    if (Number.isFinite(rawValue)) {
      return rawValue;
    }

    const obfuscatedValue = Number(result?.obfuscatedValue);
    if (Number.isFinite(obfuscatedValue)) {
      return obfuscatedValue;
    }

    return 0;
  };

  const calculatePercentage = (result) => {
    const total = Number(report.value?.numberOfEntities);
    if (!Number.isFinite(total) || total <= 0) {
      return '0.00';
    }

    return ((getOccurrenceValue(result) / total) * 100).toFixed(2);
  };

  const getResultPercentage = (result) => {
    return parseFloat(calculatePercentage(result));
  };

  const getResultStatus = (result) => {
    const percentage = getResultPercentage(result);
    if (percentage >= result.errorThreshold || result.error) {
      return CHECK_STATUS.FAILED;
    }
    if (percentage >= result.warningThreshold) {
      return CHECK_STATUS.WARNING;
    }
    return CHECK_STATUS.PASSED;
  };

  const calculateEpsilonUsed = () => {
    if (!report.value?.results) return 0;
    return report.value.results.reduce((sum, result) => sum + result.epsilon, 0);
  };

  const isOverBudget = () => {
    return calculateEpsilonUsed() > report.value.epsilonBudget;
  };

  const countErrors = () => {
    if (!report.value?.results) return 0;
    return report.value.results.filter((result) => getResultStatus(result) === CHECK_STATUS.FAILED)
      .length;
  };

  const countWarnings = () => {
    if (!report.value?.results) return 0;
    return report.value.results.filter((result) => getResultStatus(result) === CHECK_STATUS.WARNING)
      .length;
  };

  const countPassed = () => {
    if (!report.value?.results) return 0;
    return report.value.results.filter((result) => getResultStatus(result) === CHECK_STATUS.PASSED)
      .length;
  };

  const getResultClass = (result) => {
    const status = getResultStatus(result);
    if (status === CHECK_STATUS.FAILED) {
      return 'bg-danger';
    }
    if (status === CHECK_STATUS.WARNING) {
      return 'bg-warning';
    }
    return 'bg-success';
  };

  const getResultPriority = (result) => {
    const status = getResultStatus(result);
    if (status === CHECK_STATUS.FAILED) {
      return 0; // Errors first
    }
    if (status === CHECK_STATUS.WARNING) {
      return 1; // Warnings second
    }
    return 2; // Passed last
  };

  const filteredResults = computed(() => {
    if (!report.value?.results) return [];

    const statusFiltered = report.value.results.filter((result) => {
      if (!selectedStatus.value) {
        return true;
      }
      return getResultStatus(result) === selectedStatus.value;
    });

    return [...statusFiltered].sort((a, b) => {
      return getResultPriority(a) - getResultPriority(b);
    });
  });

  const scrollToCheck = async () => {
    if (route.hash) {
      // Remove the # from the hash
      const checkId = route.hash.substring(1);

      // Wait for DOM to be fully rendered
      await nextTick();

      // Small additional delay to ensure everything is rendered
      setTimeout(() => {
        const element = document.getElementById(checkId);
        if (element) {
          element.scrollIntoView({
            behavior: 'smooth',
            block: 'center',
          });

          // Add a highlight animation
          element.classList.add('highlight-check');
          setTimeout(() => {
            element.classList.remove('highlight-check');
          }, 2000);
        }
      }, 100);
    }
  };

  onMounted(async () => {
    try {
      loading.value = true;
      const reportId = route.params.id;
      report.value = await reportStore.fetchReportById(reportId);

      // Load all quality checks
      try {
        const response = await api.get('/api/quality-checks');
        qualityChecks.value = response.data._embedded?.qualityChecks || [];
      } catch (err) {
        console.error('Failed to load quality checks:', err);
        qualityChecks.value = [];
      }

      // Scroll to the specific check if hash is present
      await scrollToCheck();
    } catch (err) {
      error.value = `Failed to load report: ${err.message || 'Unknown error'}`;
    } finally {
      loading.value = false;
    }
  });
</script>

<style scoped>
  .report-detail-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .page-actions {
    display: flex;
    justify-content: flex-start;
    margin-bottom: var(--spacing-md);
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: var(--spacing-md);
  }

  .results-container {
    display: flex;
    flex-direction: column;
  }

  .filter-label {
    font-size: 0.875rem;
    color: #6c757d;
    font-weight: 500;
    margin-bottom: 0.5rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }


  .result-card {
    transition:
      transform var(--transition-base),
      box-shadow var(--transition-base);
    border: 1px solid var(--color-gray-200);
    scroll-margin-top: 100px;
  }

  .result-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .result-card.bg-success {
    background-color: var(--bg-card) !important;
    border-left: 4px solid var(--color-success);
  }

  .result-card.bg-warning {
    background-color: var(--bg-card) !important;
    border-left: 4px solid var(--color-warning);
  }

  .result-card.bg-danger {
    background-color: var(--bg-card) !important;
    border-left: 4px solid var(--color-danger);
  }

  .highlight-check {
    animation: highlightPulse 2s ease-in-out;
  }

  @keyframes highlightPulse {
    0%,
    100% {
      box-shadow: 0 0 0 rgba(102, 126, 234, 0);
    }
    50% {
      box-shadow: 0 0 20px 5px rgba(102, 126, 234, 0.5);
    }
  }

  .result-details {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);
  }

  .check-link {
    font-size: 1rem;
    font-weight: 600;
    color: var(--color-primary);
    text-decoration: none;
    cursor: pointer;
    transition: all var(--transition-base);
  }

  .check-link:hover {
    color: var(--color-primary-dark);
    text-decoration: underline;
  }

  .check-link:focus {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .check-description {
    margin-top: 0.5rem;
    padding-left: 0.5rem;
    border-left: 3px solid var(--color-gray-300);
  }

  .detail-row {
    display: flex;
    gap: var(--spacing-sm);
    font-size: 0.9rem;
  }

  .detail-label {
    font-weight: 600;
    color: var(--color-gray-500);
    min-width: 120px;
  }

  .detail-value {
    color: var(--color-gray-900);
  }

  .patient-ids-section {
    background-color: var(--color-gray-50);
    padding: var(--spacing-md);
    border-radius: var(--radius-sm);
  }

  .patient-cell {
    padding: var(--spacing-sm);
    width: 16.66%;
  }

  .patient-link {
    text-decoration: none;
    color: var(--color-primary);
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: var(--radius-sm);
    transition: background-color var(--transition-base);
    display: inline-block;
  }

  .patient-link:hover {
    background-color: var(--bg-hover);
    text-decoration: underline;
  }

  .badge {
    font-weight: 500;
    padding: var(--spacing-sm) var(--spacing-md);
    font-size: 0.875rem;
  }

  @media (max-width: 768px) {
    .report-detail-page {
      padding: var(--spacing-md);
    }

    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .detail-label {
      min-width: 100px;
      font-size: 0.85rem;
    }

    .detail-value {
      font-size: 0.85rem;
    }
  }

  @media (max-width: 576px) {
    .report-detail-page {
      padding: 0.75rem;
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }

    .patient-cell {
      width: 33.33%;
    }
  }
</style>
