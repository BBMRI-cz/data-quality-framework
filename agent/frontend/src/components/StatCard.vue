<template>
  <div class="stat-card">
    <div v-if="helpText" class="help-icon-container">
      <i class="bi bi-info-circle-fill help-icon"></i>
      <div class="tooltip-text">{{ helpText }}</div>
    </div>
    <div class="stat-number" :class="numberClass">{{ number }}</div>
    <div class="stat-label">{{ label }}</div>
  </div>
</template>

<script setup>
  defineProps({
    number: {
      type: [String, Number],
      required: true,
    },
    label: {
      type: String,
      required: true,
    },
    numberClass: {
      type: String,
      default: 'text-dark',
    },
    helpText: {
      type: String,
      default: '',
    },
  });
</script>

<style scoped>
  .stat-card {
    position: relative;
    background: var(--bg-card);
    border-radius: var(--radius-md);
    padding: 1.25rem;
    box-shadow: var(--shadow-sm);
    transition:
      transform var(--transition-base),
      box-shadow var(--transition-base);
  }

  .stat-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .stat-number {
    font-size: 1.75rem;
    font-weight: 700;
    line-height: 1.2;
    margin-bottom: 0.25rem;
  }

  .stat-label {
    font-size: 0.813rem;
    color: var(--color-gray-500);
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .help-icon-container {
    position: absolute;
    top: 1rem;
    right: 1rem;
  }

  .help-icon {
    font-size: 1.125rem;
    color: var(--color-gray-400);
    cursor: help;
    transition: color var(--transition-base);
  }

  .help-icon:hover {
    color: var(--color-primary);
  }

  .tooltip-text {
    visibility: hidden;
    opacity: 0;
    position: absolute;
    top: calc(100% + 0.5rem);
    right: 0;
    background-color: rgba(0, 0, 0, 0.9);
    color: white;
    text-align: left;
    padding: 0.5rem 0.75rem;
    border-radius: var(--radius-sm);
    font-size: 0.813rem;
    font-weight: 400;
    white-space: normal;
    width: max-content;
    max-width: 250px;
    z-index: 1000;
    box-shadow: var(--shadow-md);
    transition:
      opacity 0.05s ease,
      visibility 0.05s ease;
    pointer-events: none;
    line-height: 1.4;
    text-transform: none;
    letter-spacing: normal;
  }

  .tooltip-text::before {
    content: '';
    position: absolute;
    bottom: 100%;
    right: 0.5rem;
    border-width: 0.25rem;
    border-style: solid;
    border-color: transparent transparent rgba(0, 0, 0, 0.9) transparent;
  }

  .help-icon-container:hover .tooltip-text {
    visibility: visible;
    opacity: 1;
  }
</style>
