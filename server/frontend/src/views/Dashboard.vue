<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1>Data Quality Dashboard</h1>
      <p>Monitor and analyze your data quality metrics</p>
    </div>

    <div class="dashboard-grid">
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4zm2.5 2.5h-15V5h15v14.5zm0-16.5h-15c-.83 0-1.5.67-1.5 1.5v15c0 .83.67 1.5 1.5 1.5h15c.83 0 1.5-.67 1.5-1.5v-15c0-.83-.67-1.5-1.5-1.5z" fill="currentColor"/>
          </svg>
        </div>
        <div class="metric-content">
          <h3>Data Quality Score</h3>
          <div class="metric-value">87%</div>
          <div class="metric-change positive">+2.3% from last week</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" fill="currentColor"/>
          </svg>
        </div>
        <div class="metric-content">
          <h3>Issues Detected</h3>
          <div class="metric-value">23</div>
          <div class="metric-change negative">+5 from yesterday</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" fill="currentColor"/>
          </svg>
        </div>
        <div class="metric-content">
          <h3>Records Processed</h3>
          <div class="metric-value">1.2M</div>
          <div class="metric-change positive">+15% from last month</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" fill="currentColor"/>
          </svg>
        </div>
        <div class="metric-content">
          <h3>Data Accuracy</h3>
          <div class="metric-value">94.2%</div>
          <div class="metric-change positive">+1.1% from last week</div>
        </div>
      </div>
    </div>

    <div class="dashboard-section">
      <h2>Recent Activity</h2>
      <div class="activity-list">
        <div class="activity-item">
          <div class="activity-icon">✓</div>
          <div class="activity-content">
            <span class="activity-title">Data validation completed</span>
            <span class="activity-time">2 minutes ago</span>
          </div>
        </div>
        <div class="activity-item">
          <div class="activity-icon">⚠</div>
          <div class="activity-content">
            <span class="activity-title">Quality issue detected in dataset ABC</span>
            <span class="activity-time">15 minutes ago</span>
          </div>
        </div>
        <div class="activity-item">
          <div class="activity-icon">📊</div>
          <div class="activity-content">
            <span class="activity-title">Weekly report generated</span>
            <span class="activity-time">1 hour ago</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Dashboard logic can be added here
</script>

<style scoped>
.dashboard {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  margin-bottom: 2rem;
}

.dashboard-header h1 {
  color: #1f2937;
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 700;
}

.dashboard-header p {
  color: #6b7280;
  margin: 0;
  font-size: 1.125rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 3rem;
}

.metric-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 1rem;
}

.metric-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.metric-content h3 {
  margin: 0 0 0.5rem 0;
  color: #6b7280;
  font-size: 0.875rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.metric-value {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.metric-change {
  font-size: 0.875rem;
  font-weight: 500;
}

.metric-change.positive {
  color: #059669;
}

.metric-change.negative {
  color: #dc2626;
}

.dashboard-section {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.dashboard-section h2 {
  margin: 0 0 1.5rem 0;
  color: #1f2937;
  font-size: 1.25rem;
  font-weight: 600;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  border-radius: 8px;
  background: #f8fafc;
}

.activity-icon {
  width: 32px;
  height: 32px;
  background: #e5e7eb;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

.activity-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.activity-title {
  font-weight: 500;
  color: #1f2937;
}

.activity-time {
  font-size: 0.875rem;
  color: #6b7280;
}
</style>
