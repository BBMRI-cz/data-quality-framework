<template>
  <div
    v-if="report"
    class="modal fade show d-block modal-backdrop-custom"
    tabindex="-1"
    @click.self="$emit('close')"
  >
    <div class="modal-dialog modal-lg modal-dialog-scrollable modal-dialog-centered">
      <div class="modal-content border-0 shadow-lg">
        <div class="modal-header bg-gradient-purple border-0 text-white py-3">
          <div class="d-flex align-items-center">
            <div class="icon-wrapper me-3">
              <i class="bi bi-file-earmark-text fs-4"></i>
            </div>
            <div>
              <h5 class="modal-title mb-0">Report Details</h5>
              <small class="opacity-90 font-monospace">{{ report.id }}</small>
            </div>
          </div>
          <button
            type="button"
            class="btn-close btn-close-custom"
            aria-label="Close"
            @click="$emit('close')"
          ></button>
        </div>

        <div class="modal-body p-0">
          <!-- Report Summary -->
          <div class="report-summary p-4 bg-light border-bottom">
            <div class="row g-3">
              <div class="col-md-4">
                <div class="summary-card">
                  <i class="bi bi-clock-history text-primary"></i>
                  <div>
                    <small class="text-muted d-block">Timestamp</small>
                    <strong>{{ formatDateShort(report.timestamp) }}</strong>
                    <div class="text-muted small">{{ formatTime(report.timestamp) }}</div>
                  </div>
                </div>
              </div>
              <div class="col-md-4">
                <div class="summary-card">
                  <i class="bi bi-list-check text-info"></i>
                  <div>
                    <small class="text-muted d-block">Total Checks</small>
                    <strong class="fs-5">{{ getCheckCounts().total }}</strong>
                  </div>
                </div>
              </div>
              <div class="col-md-4">
                <div class="summary-card">
                  <i class="bi bi-clipboard-check text-info"></i>
                  <div>
                    <small class="text-muted d-block">Overall Status</small>
                    <span class="badge" :class="getOverallStatusBadgeClass()">
                      {{ getOverallStatus() }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Stats Row -->
            <div class="row g-2 mt-3">
              <div class="col-4">
                <div class="stat-box stat-success">
                  <i class="bi bi-check-circle"></i>
                  <div class="stat-value">{{ getCheckCounts().passed }}</div>
                  <div class="stat-label">Passed</div>
                </div>
              </div>
              <div class="col-4">
                <div class="stat-box stat-warning">
                  <i class="bi bi-exclamation-triangle"></i>
                  <div class="stat-value">{{ getCheckCounts().warnings }}</div>
                  <div class="stat-label">Warnings</div>
                </div>
              </div>
              <div class="col-4">
                <div class="stat-box stat-danger">
                  <i class="bi bi-x-circle"></i>
                  <div class="stat-value">{{ getCheckCounts().failed }}</div>
                  <div class="stat-label">Errors</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Checks List -->
          <div class="checks-container custom-scrollbar">
            <div class="checks-header px-4 py-3 bg-white border-bottom">
              <h6
                class="mb-0 text-uppercase text-muted fw-semibold"
                style="font-size: 0.75rem; letter-spacing: 0.5px"
              >
                Quality Check Results
              </h6>
            </div>

            <div class="list-group list-group-flush">
              <div
                v-if="!report.results || report.results.length === 0"
                class="p-5 text-center text-muted"
              >
                <i class="bi bi-inbox fs-1 d-block mb-3 opacity-50"></i>
                <p class="mb-0">No check results available for this report</p>
              </div>

              <div
                v-for="result in report.results"
                :key="result.hash"
                class="list-group-item border-0 border-bottom check-item"
              >
                <div class="d-flex justify-content-between align-items-start gap-3">
                  <div class="flex-grow-1">
                    <div class="d-flex align-items-start gap-2 mb-2">
                      <i class="bi fs-5 mt-1" :class="getCheckIcon(result)"></i>
                      <div class="flex-grow-1">
                        <h6 class="mb-1 fw-semibold">{{ getCheckName(result.hash) }}</h6>
                        <p class="mb-2 small text-muted">{{ getCheckDescription(result.hash) }}</p>
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                          <span class="badge" :class="getCheckBadgeClass(result)">
                            {{ getCheckStatusText(result) }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="check-score">
                    <div class="score-circle" :class="getScoreCircleClass(result)">
                      <span class="score-value">{{ (result.result * 100).toFixed(1) }}%</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer bg-light border-0 py-3 d-flex justify-content-center">
          <button type="button" class="btn btn-close-footer" @click="$emit('close')">
            <i class="bi bi-x-circle me-2"></i>
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import {
    getCheckStatus,
    getStatusBadgeClass,
    formatScore,
    countChecksByStatus,
    getReportStatus,
    CheckStatus,
  } from '../utils/qualityCheckUtils.js';

  const props = defineProps({
    report: {
      type: Object,
      default: null,
    },
    qualityCheckMap: {
      type: Map,
      required: true,
    },
  });

  defineEmits(['close']);

  const formatDateShort = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getCheckName = (hash) => {
    const check = props.qualityCheckMap.get(hash);
    return check?.name || hash;
  };

  const getCheckDescription = (hash) => {
    const check = props.qualityCheckMap.get(hash);
    return check?.description || 'No description available';
  };

  const getCheckStatusText = (result) => {
    const check = props.qualityCheckMap.get(result.hash);
    return getCheckStatus(result, check);
  };

  const getCheckBadgeClass = (result) => {
    const status = getCheckStatusText(result);
    return getStatusBadgeClass(status);
  };

  const getCheckIcon = (result) => {
    const status = getCheckStatusText(result);
    switch (status) {
      case CheckStatus.PASSED:
        return 'bi-check-circle-fill text-success';
      case CheckStatus.WARNING:
        return 'bi-exclamation-triangle-fill text-warning';
      case CheckStatus.FAILED:
        return 'bi-x-circle-fill text-danger';
      default:
        return 'bi-question-circle text-secondary';
    }
  };

  const getScoreCircleClass = (result) => {
    const status = getCheckStatusText(result);
    switch (status) {
      case CheckStatus.PASSED:
        return 'score-success';
      case CheckStatus.WARNING:
        return 'score-warning';
      case CheckStatus.FAILED:
        return 'score-danger';
      default:
        return 'score-secondary';
    }
  };

  const getCheckCounts = () => {
    return countChecksByStatus(props.report, props.qualityCheckMap);
  };

  const getOverallStatus = () => {
    return getReportStatus(props.report, props.qualityCheckMap);
  };

  const getOverallStatusBadgeClass = () => {
    const status = getOverallStatus();
    return getStatusBadgeClass(status);
  };
</script>

<style scoped>
  .modal-backdrop-custom {
    background-color: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(2px);
  }

  .modal-dialog-centered {
    display: flex;
    align-items: center;
    min-height: calc(100% - 3.5rem);
  }

  .modal-content {
    border-radius: 0.75rem;
    overflow: hidden;
  }

  .bg-gradient-primary {
    background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);
  }

  .bg-gradient-purple {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  .btn-close-custom {
    background: transparent;
    border: none;
    width: 32px;
    height: 32px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.375rem;
    transition: all 0.2s ease;
    position: relative;
    opacity: 1;
  }

  .btn-close-custom::before,
  .btn-close-custom::after {
    content: '';
    position: absolute;
    width: 18px;
    height: 2px;
    background-color: white;
    border-radius: 1px;
    transition: all 0.2s ease;
  }

  .btn-close-custom::before {
    transform: rotate(45deg);
  }

  .btn-close-custom::after {
    transform: rotate(-45deg);
  }

  .btn-close-custom:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: scale(1.1);
  }

  .btn-close-custom:hover::before,
  .btn-close-custom:hover::after {
    background-color: white;
    width: 20px;
  }

  .btn-close-custom:active {
    transform: scale(0.95);
  }

  .icon-wrapper {
    width: 50px;
    height: 50px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .font-monospace {
    font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
    font-size: 0.875rem;
  }

  .report-summary {
    background: linear-gradient(to bottom, #f8f9fa 0%, #ffffff 100%);
  }

  .summary-card {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .summary-card i {
    font-size: 1.5rem;
  }

  .stat-box {
    text-align: center;
    padding: 1rem;
    border-radius: 0.5rem;
    background: white;
    border: 1px solid #e9ecef;
    transition: all 0.2s ease;
  }

  .stat-box:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }

  .stat-box i {
    font-size: 1.5rem;
    margin-bottom: 0.5rem;
  }

  .stat-value {
    font-size: 1.5rem;
    font-weight: 700;
    line-height: 1;
  }

  .stat-label {
    font-size: 0.75rem;
    color: #6c757d;
    margin-top: 0.25rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .stat-success i {
    color: #198754;
  }
  .stat-warning i {
    color: #ffc107;
  }
  .stat-danger i {
    color: #dc3545;
  }

  .checks-container {
    max-height: 500px;
    overflow-y: auto;
  }

  .checks-header {
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .check-item {
    padding: 1.25rem 1.5rem;
    transition: background-color 0.2s ease;
  }

  .check-item:hover {
    background-color: #f8f9fa;
  }

  .check-score {
    flex-shrink: 0;
  }

  .score-circle {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    border: 3px solid;
  }

  .score-value {
    font-size: 1.25rem;
    line-height: 1;
  }

  .score-label {
    font-size: 0.625rem;
    font-weight: 600;
    text-transform: uppercase;
    margin-top: 0.125rem;
    opacity: 0.8;
  }

  .score-success {
    background-color: #d1e7dd;
    border-color: #198754;
    color: #0f5132;
  }

  .score-warning {
    background-color: #fff3cd;
    border-color: #ffc107;
    color: #664d03;
  }

  .score-danger {
    background-color: #f8d7da;
    border-color: #dc3545;
    color: #842029;
  }

  .score-secondary {
    background-color: #e9ecef;
    border-color: #6c757d;
    color: #495057;
  }

  .badge {
    font-weight: 600;
    padding: 0.35rem 0.75rem;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  /* Footer close button */
  .btn-close-footer {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border: none;
    padding: 0.75rem 2rem;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.95rem;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .btn-close-footer:hover {
    background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    color: white;
  }

  .btn-close-footer:active {
    transform: translateY(0);
    box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
  }

  .btn-close-footer i {
    font-size: 1.1rem;
  }

  /* Responsive adjustments */
  @media (max-width: 768px) {
    .modal-dialog {
      margin: 0.5rem;
    }

    .report-summary {
      padding: 1rem !important;
    }

    .stat-box {
      padding: 0.75rem;
    }

    .stat-value {
      font-size: 1.25rem;
    }

    .check-item {
      padding: 1rem;
    }

    .score-circle {
      width: 50px;
      height: 50px;
      font-size: 0.75rem;
    }
  }
</style>
