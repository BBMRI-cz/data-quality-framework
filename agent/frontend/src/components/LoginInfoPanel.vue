<template>
  <div class="info-panel">
    <div>
      <h1 class="info-panel__title">Data Quality Agent</h1>
      <p class="info-panel__lead">Local Repository Data Quality Monitoring</p>

      <div class="info-panel__features">
        <div class="info-panel__feature">
          <h5 class="info-panel__feature-title">Local Data Validation</h5>
          <p class="info-panel__feature-text">
            Performs comprehensive Data Quality Checks directly on your local repository.
          </p>
        </div>

        <div class="info-panel__feature">
          <h5 class="info-panel__feature-title">Privacy Preserving Data Quality Reports</h5>
          <p class="info-panel__feature-text">
            Can periodically generate Data Quality Reports for a central Data Quality Server.
          </p>
        </div>
      </div>
    </div>

    <div class="info-panel__stats">
      <div class="info-panel__stat">
        <div class="info-panel__stat-value">Custom</div>
        <small class="info-panel__stat-label">Quality Checks</small>
      </div>
      <div class="info-panel__stat">
        <div class="info-panel__stat-value">Auto</div>
        <small class="info-panel__stat-label">Reporting</small>
      </div>
    </div>
  </div>
</template>

<script setup></script>

<style scoped>
  .info-panel {
    background: var(--gradient-primary);
    color: white;
    padding: var(--spacing-xl);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 100%;
  }

  .info-panel__title {
    font-size: 2.5rem;
    font-weight: bold;
    margin-bottom: var(--spacing-md);
    color: white;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .info-panel__lead {
    font-size: 1.25rem;
    margin-bottom: var(--spacing-lg);
    color: rgba(255, 255, 255, 0.95);
  }

  .info-panel__features {
    margin-bottom: var(--spacing-lg);
  }

  .info-panel__feature {
    margin-bottom: var(--spacing-lg);
  }

  .info-panel__feature-title {
    font-weight: 600;
    margin-bottom: var(--spacing-sm);
    color: white;
  }

  .info-panel__feature-text {
    color: rgba(255, 255, 255, 0.85);
    margin-bottom: 0;
  }

  .info-panel__stats {
    border-top: 1px solid rgba(255, 255, 255, 0.2);
    padding-top: var(--spacing-lg);
    display: flex;
    text-align: center;
  }

  .info-panel__stat {
    flex: 1;
  }

  .info-panel__stat-value {
    font-size: 1.5rem;
    font-weight: bold;
    margin-bottom: var(--spacing-xs);
  }

  .info-panel__stat-label {
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.85);
  }

  @media (min-width: 1200px) {
    .info-panel {
      padding: var(--spacing-2xl);
    }
  }
</style>
