import { computed } from 'vue';
import { CheckStatus } from '@/utils/qualityCheckUtils.js';

const STATUS_META = {
  [CheckStatus.PASSED]: {
    value: CheckStatus.PASSED,
    label: 'Passed',
    color: 'var(--color-success)',
    icon: 'bi bi-check-circle-fill',
  },
  [CheckStatus.WARNING]: {
    value: CheckStatus.WARNING,
    label: 'Warning',
    color: 'var(--color-warning)',
    icon: 'bi bi-exclamation-circle-fill',
  },
  [CheckStatus.FAILED]: {
    value: CheckStatus.FAILED,
    label: 'Failed',
    color: 'var(--color-danger)',
    icon: 'bi bi-exclamation-triangle-fill',
  },
  [CheckStatus.NO_DATA]: {
    value: CheckStatus.NO_DATA,
    label: 'No data',
    color: 'var(--color-gray-500)',
    icon: 'bi bi-question-circle-fill',
  },
  [CheckStatus.UNKNOWN]: {
    value: CheckStatus.UNKNOWN,
    label: 'Unknown',
    color: 'var(--color-gray-500)',
    icon: 'bi bi-question-circle-fill',
  },
};

const DEFAULT_STATUS_META = STATUS_META[CheckStatus.UNKNOWN];

export function useStatuses() {
  const allowedValues = computed(() => [
    CheckStatus.PASSED,
    CheckStatus.WARNING,
    CheckStatus.FAILED,
  ]);

  const statusOptions = computed(() => {
    return allowedValues.value.map((status) => {
      const meta = STATUS_META[status];
      return {
        value: meta.value,
        label: meta.label,
        color: meta.color,
      };
    });
  });

  const getStatusMeta = (status) => {
    const base = STATUS_META[status] || DEFAULT_STATUS_META;

    return {
      ...base,
      iconStyle: { color: base.color },
    };
  };

  return {
    allowedValues,
    statusOptions,
    getStatusMeta,
  };
}
