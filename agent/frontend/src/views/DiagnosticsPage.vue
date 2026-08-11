<template>
  <div class="diagnostics-page">
    <PageHeader
      title="Diagnostics"
      mobile-title="Diagnostics"
      subtitle="Inspect health and recent logs"
      icon="bi bi-activity"
    >
      <template #actions>
        <ActionButton
          :loading="isLoading"
          icon="bi bi-arrow-clockwise"
          text="Refresh"
          @click="loadAll"
        />
      </template>
    </PageHeader>

    <div class="page-content">
      <!-- Health -->
      <div class="card diagnostics-card mb-4 border-0 shadow-sm">
        <div class="card-header bg-white d-flex justify-content-between align-items-center">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-heart-pulse text-primary fs-5"></i>
            <h5 class="mb-0">Health</h5>
          </div>
          <span v-if="health" class="badge rounded-pill health-badge" :class="statusClass(health.status)">
            {{ health.status }}
          </span>
        </div>
        <div class="card-body">
          <p v-if="healthError" class="text-danger mb-0">
            <i class="bi bi-exclamation-triangle me-1"></i>{{ healthError }}
          </p>
          <div v-else-if="healthComponents.length" class="d-flex flex-wrap gap-2">
            <span
              v-for="component in healthComponents"
              :key="component.name"
              class="badge rounded-pill health-badge"
              :class="statusClass(component.status)"
            >
              {{ component.name }}
              <span v-if="component.details" class="detail-toggle">
                <i class="bi bi-info-circle"></i>
                <span class="detail-tooltip">{{ component.details }}</span>
              </span>
            </span>
          </div>
          <p v-else class="text-muted mb-0">
            <i class="bi bi-inbox me-1"></i>No health components available.
          </p>
        </div>
      </div>

      <!-- Log Level -->
      <div class="card diagnostics-card mb-4 border-0 shadow-sm">
        <div class="card-header bg-white">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-sliders text-primary fs-5"></i>
            <h5 class="mb-0">Log Level</h5>
          </div>
        </div>
        <div class="card-body">
          <div class="d-flex align-items-end flex-wrap gap-2">
            <div class="level-control">
              <label class="form-label mb-1" for="logLevel">Level</label>
              <select id="logLevel" v-model="selectedLevel" class="form-select">
                <option v-for="level in levels" :key="level" :value="level">{{ level }}</option>
              </select>
            </div>
            <ActionButton
              :loading="isSettingLevel"
              icon="bi bi-check-circle"
              text="Apply"
              @click="applyLogLevel"
            />
          </div>
          <p class="text-muted small mt-2 mb-0">
            <i class="bi bi-braces me-1"></i>Target logger:
            <code class="logger-name">{{ LOGGER_NAME }}</code>
          </p>
          <div v-if="loggerError" class="text-danger small mt-2 mb-0">
            <i class="bi bi-exclamation-triangle me-1"></i>{{ loggerError }}
          </div>
        </div>
      </div>

      <!-- Logs -->
      <div class="card diagnostics-card border-0 shadow-sm">
        <div class="card-header bg-white">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-list-ul text-primary fs-5"></i>
            <h5 class="mb-0">Recent Logs</h5>
            <span v-if="logs.length" class="badge text-bg-secondary rounded-pill ms-1">{{
              logs.length
            }}</span>
          </div>
        </div>
        <div class="card-body p-0">
          <p v-if="logsError" class="text-danger mb-0 p-3">
            <i class="bi bi-exclamation-triangle me-1"></i>{{ logsError }}
          </p>
          <div v-else-if="logs.length === 0 && !isLoading" class="p-3">
            <p class="text-muted mb-0">
              <i class="bi bi-inbox me-1"></i>No log entries captured yet.
            </p>
          </div>
          <div v-else class="table-responsive logs-table">
            <table class="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th class="text-nowrap">Time</th>
                  <th>Level</th>
                  <th>Logger</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(log, index) in sortedLogs" :key="index">
                  <td class="text-nowrap text-muted log-time">{{ formatTime(log.timestamp) }}</td>
                  <td>
                    <span class="badge level-badge" :class="levelClass(log.level)">
                      {{ log.level }}
                    </span>
                  </td>
                  <td class="log-logger">{{ log.loggerName }}</td>
                  <td class="log-message">{{ log.message }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
  import PageHeader from '@/components/PageHeader.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import { diagnosticsService } from '@/services/diagnosticsService.js';

  const health = ref(null);
  const healthError = ref('');
  const logs = ref([]);
  const logsError = ref('');
  const isLoading = ref(false);

  const levels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'];
  const LOGGER_NAME = 'eu.bbmri_eric.quality.agent';
  const selectedLevel = ref('INFO');
  const loggerInfo = ref(null);
  const loggerError = ref('');
  const isSettingLevel = ref(false);

  const statusClass = (status) => {
    const key = (status || '').toUpperCase();
    if (key === 'UP') return 'badge-success';
    if (key === 'DOWN') return 'badge-danger';
    if (key === 'OUT_OF_SERVICE') return 'badge-warning';
    return 'badge-muted';
  };

  const formatDetails = (details) => {
    if (details == null) return '';
    if (typeof details === 'object') {
      return Object.entries(details)
        .map(([key, value]) => {
          const formatted =
            value && typeof value === 'object' ? JSON.stringify(value) : String(value);
          return `${key}: ${formatted}`;
        })
        .join('\n');
    }
    return String(details);
  };

  const healthComponents = computed(() => {
    const components = health.value?.components;
    if (!components || typeof components !== 'object') return [];
    return Object.entries(components).map(([name, component]) => ({
      name,
      status: component.status || 'UNKNOWN',
      details: formatDetails(component.details),
    }));
  });

  const levelClass = (level) => {
    const key = (level || '').toUpperCase();
    if (key === 'ERROR' || key === 'FATAL') return 'bg-danger';
    if (key === 'WARN') return 'bg-warning text-dark';
    if (key === 'DEBUG' || key === 'TRACE') return 'bg-secondary';
    return 'bg-info text-dark';
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    return new Date(timestamp).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  async function refreshLoggerInfo() {
    try {
      loggerInfo.value = await diagnosticsService.getLogger(LOGGER_NAME);
      const current = loggerInfo.value.configuredLevel || loggerInfo.value.effectiveLevel;
      if (current && levels.includes(current)) {
        selectedLevel.value = current;
      }
      loggerError.value = '';
    } catch {
      loggerInfo.value = null;
      loggerError.value = 'Unable to read the current level for this logger.';
    }
  }

  const sortedLogs = computed(() => {
    return [...logs.value].sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );
  });

  async function applyLogLevel() {
    isSettingLevel.value = true;
    try {
      await diagnosticsService.setLogLevel(LOGGER_NAME, selectedLevel.value);
      await refreshLoggerInfo();
    } catch {
      loggerError.value = 'Unable to update the log level.';
    } finally {
      isSettingLevel.value = false;
    }
  }

  let logsTimer = null;
  let isFetchingLogs = false;

  async function loadLogs() {
    if (isFetchingLogs) return;
    isFetchingLogs = true;
    try {
      const data = await diagnosticsService.getLogs();
      logs.value = Array.isArray(data) ? data : [];
      logsError.value = '';
    } catch {
      logsError.value = 'Unable to load logs.';
    } finally {
      isFetchingLogs = false;
    }
  }

  async function loadAll() {
    isLoading.value = true;
    try {
      const [healthResult] = await Promise.allSettled([diagnosticsService.getHealth()]);
      if (healthResult.status === 'fulfilled') {
        health.value = healthResult.value;
        healthError.value = '';
      } else {
        health.value = null;
        healthError.value = 'Unable to load health endpoint.';
      }
      await loadLogs();
    } finally {
      isLoading.value = false;
    }
  }

  onMounted(() => {
    loadAll();
    refreshLoggerInfo();
    logsTimer = setInterval(loadLogs, 5000);
  });

  onBeforeUnmount(() => {
    if (logsTimer) {
      clearInterval(logsTimer);
    }
  });
</script>

<style scoped>
  .diagnostics-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .diagnostics-card {
    border-radius: var(--radius-lg);
  }

  .level-control {
    min-width: 220px;
    max-width: 240px;
  }

  .logger-name {
    font-family: var(--font-mono), monospace;
    font-size: 0.75rem;
    color: var(--color-gray-700);
  }

  .card-header {
    border-bottom: 1px solid var(--color-gray-100);
  }

  .health-badge {
    font-size: 0.75rem;
    font-weight: 600;
    color: white;
  }

  .badge-success {
    background: var(--color-success);
  }

  .badge-danger {
    background: var(--color-danger);
  }

  .badge-warning {
    background: var(--color-warning);
    color: var(--color-gray-800);
  }

  .badge-muted {
    background: var(--color-gray-500);
  }

  .detail-toggle {
    position: relative;
    display: inline-flex;
    align-items: center;
    margin-left: 0.25rem;
    cursor: help;
  }

  .detail-tooltip {
    visibility: hidden;
    opacity: 0;
    position: absolute;
    bottom: 140%;
    left: 50%;
    transform: translateX(-50%) translateY(4px);
    background: var(--color-gray-800);
    color: white;
    font-family: var(--font-mono), monospace;
    font-size: 0.6875rem;
    line-height: 1.5;
    padding: 0.5rem 0.625rem;
    border-radius: var(--radius-md);
    white-space: pre-wrap;
    word-break: break-word;
    min-width: 220px;
    max-width: 380px;
    max-height: 300px;
    overflow: auto;
    z-index: 10;
    box-shadow: var(--shadow-lg);
    transition: opacity var(--transition-fast), transform var(--transition-fast);
    pointer-events: none;
  }

  .detail-toggle:hover .detail-tooltip {
    visibility: visible;
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }

  .logs-table {
    max-height: 480px;
    overflow-y: auto;
  }

  .logs-table thead th {
    position: sticky;
    top: 0;
    background: var(--color-gray-50);
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--color-gray-500);
    border-bottom: 1px solid var(--color-gray-200);
  }

  .level-badge {
    font-size: 0.7rem;
    font-weight: 600;
  }

  .log-logger {
    font-size: 0.8125rem;
    color: var(--color-gray-600);
    max-width: 320px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .log-message {
    font-size: 0.875rem;
    color: var(--color-gray-800);
  }

  @media (max-width: 768px) {
    .diagnostics-page {
      padding: var(--spacing-md);
    }
  }

  @media (max-width: 576px) {
    .diagnostics-page {
      padding: 0.75rem;
    }
  }
</style>
