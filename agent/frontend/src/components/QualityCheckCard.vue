<template>
  <div class="card border-0 shadow-sm h-100 quality-check-card clickable" @click="navigateToCheck">
    <div class="card-body p-3 position-relative d-flex flex-column">
      <!-- Icon in top right corner -->
      <div class="position-absolute top-0 end-0 p-3">
        <i
          class="bi bi-check-square"
          :class="getResultColorClass(check)"
          style="font-size: 1.5rem; opacity: 0.75"
        ></i>
      </div>

      <!-- Check Name -->
      <div class="mb-3 pe-5">
        <p
          class="text-muted mb-0 fw-bold text-truncate"
          style="font-size: 1rem; line-height: 1.3"
          :title="check.checkName"
        >
          {{ check.checkName }}
        </p>
        <small
          v-if="check.stratum"
          class="text-muted d-block text-truncate"
          style="font-size: 0.7rem; opacity: 0.6"
          :title="check.stratum"
        >
          {{ check.stratum }}
        </small>
      </div>

      <!-- Main Result Display -->
      <div class="flex-grow-1 d-flex flex-column justify-content-center align-items-center mb-3">
        <div
          class="display-4 fw-bold mb-2"
          :class="getResultColorClass(check)"
          style="line-height: 1"
        >
          {{ formatPercentage(check.rawValue) }}%
        </div>
        <div class="text-muted" style="font-size: 0.9rem">Occurrence Rate</div>
      </div>

      <!-- Additional Info -->
      <div class="mt-auto">
        <!-- Threshold Info -->
        <div class="pt-2 border-top">
          <div class="d-flex justify-content-between align-items-center" style="font-size: 0.85rem">
            <span class="text-muted">Thresholds:</span>
            <span>
              <span class="text-warning fw-bold">{{ check.warningThreshold }}%</span>
              <span class="text-muted mx-1">/</span>
              <span class="text-danger fw-bold">{{ check.errorThreshold }}%</span>
            </span>
          </div>
        </div>

        <!-- Error Message if present -->
        <div v-if="check.error" class="alert alert-danger p-2 mt-2 mb-0" style="font-size: 0.75rem">
          <i class="bi bi-exclamation-triangle-fill me-1"></i>
          {{ check.error }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { useRouter } from 'vue-router';

  const router = useRouter();

  const props = defineProps({
    check: {
      type: Object,
      required: true,
    },
    totalEntities: {
      type: Number,
      required: true,
    },
    reportId: {
      type: [String, Number],
      required: true,
    },
  });

  const formatPercentage = (value) => {
    const percentage = (value / props.totalEntities) * 100;
    return percentage.toFixed(1);
  };

  const getResultColorClass = (check) => {
    const percentage = parseFloat(formatPercentage(check.rawValue));

    if (percentage >= check.errorThreshold || check.error) {
      return 'text-danger';
    } else if (percentage >= check.warningThreshold) {
      return 'text-warning';
    }
    return 'text-success';
  };

  const getCheckIdKey = (check) => {
    return check.checkId + '_' + (check.stratum || 'all');
  };

  const navigateToCheck = () => {
    const checkId = getCheckIdKey(props.check);
    router.push({
      path: `/reports/${props.reportId}`,
      hash: `#${checkId}`,
    });
  };
</script>

<style scoped>
  .quality-check-card {
    transition:
      transform var(--transition-base),
      box-shadow var(--transition-base);
    will-change: transform, box-shadow;
  }

  .quality-check-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg) !important;
  }

  .clickable {
    cursor: pointer;
  }

  /* Responsive adjustments for smaller screens */
  @media (max-width: 576px) {
    .quality-check-card .card-body {
      padding: 0.75rem !important;
    }

    .quality-check-card .pe-5 {
      padding-right: 3.5rem !important;
    }

    .quality-check-card .position-absolute.top-0.end-0 {
      padding: 0.5rem !important;
    }

    .quality-check-card .position-absolute i {
      font-size: 1.25rem !important;
    }
  }

  @media (max-width: 400px) {
    .quality-check-card .pe-5 {
      padding-right: 4rem !important;
    }

    .quality-check-card .text-truncate {
      max-width: calc(100% - 0.5rem);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
</style>
