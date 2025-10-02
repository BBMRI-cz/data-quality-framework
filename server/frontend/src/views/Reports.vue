<template>
  <div class="reports">
    <div class="reports-header">
      <h1>Data Quality Reports</h1>
      <p>View and analyze detailed data quality reports</p>
    </div>

    <div class="reports-toolbar">
      <div class="filter-section">
        <select class="filter-select" v-model="selectedTimeframe">
          <option value="week">Last Week</option>
          <option value="month">Last Month</option>
          <option value="quarter">Last Quarter</option>
          <option value="year">Last Year</option>
        </select>
        <select class="filter-select" v-model="selectedDataset">
          <option value="all">All Datasets</option>
          <option value="patient">Patient Data</option>
          <option value="biobank">Biobank Data</option>
          <option value="clinical">Clinical Data</option>
        </select>
      </div>
      <button class="generate-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" fill="currentColor"/>
        </svg>
        Generate Report
      </button>
    </div>

    <div class="reports-grid">
      <div class="report-card">
        <div class="report-header">
          <h3>Weekly Quality Summary</h3>
          <span class="report-date">Oct 1, 2025</span>
        </div>
        <div class="report-stats">
          <div class="stat">
            <span class="stat-label">Overall Score</span>
            <span class="stat-value good">87%</span>
          </div>
          <div class="stat">
            <span class="stat-label">Issues Found</span>
            <span class="stat-value warning">23</span>
          </div>
          <div class="stat">
            <span class="stat-label">Records Checked</span>
            <span class="stat-value">1.2M</span>
          </div>
        </div>
        <div class="report-actions">
          <button class="btn-secondary">View Details</button>
          <button class="btn-primary">Download PDF</button>
        </div>
      </div>

      <div class="report-card">
        <div class="report-header">
          <h3>Patient Data Validation</h3>
          <span class="report-date">Sep 28, 2025</span>
        </div>
        <div class="report-stats">
          <div class="stat">
            <span class="stat-label">Completeness</span>
            <span class="stat-value good">94%</span>
          </div>
          <div class="stat">
            <span class="stat-label">Accuracy</span>
            <span class="stat-value good">92%</span>
          </div>
          <div class="stat">
            <span class="stat-label">Consistency</span>
            <span class="stat-value warning">78%</span>
          </div>
        </div>
        <div class="report-actions">
          <button class="btn-secondary">View Details</button>
          <button class="btn-primary">Download PDF</button>
        </div>
      </div>

      <div class="report-card">
        <div class="report-header">
          <h3>Biobank Quality Check</h3>
          <span class="report-date">Sep 25, 2025</span>
        </div>
        <div class="report-stats">
          <div class="stat">
            <span class="stat-label">Data Integrity</span>
            <span class="stat-value good">96%</span>
          </div>
          <div class="stat">
            <span class="stat-label">Missing Values</span>
            <span class="stat-value error">12%</span>
          </div>
          <div class="stat">
            <span class="stat-label">Format Errors</span>
            <span class="stat-value warning">5%</span>
          </div>
        </div>
        <div class="report-actions">
          <button class="btn-secondary">View Details</button>
          <button class="btn-primary">Download PDF</button>
        </div>
      </div>
    </div>

    <div class="trends-section">
      <h2>Quality Trends</h2>
      <div class="trends-chart">
        <div class="chart-placeholder">
          <svg width="100%" height="200" viewBox="0 0 600 200">
            <defs>
              <linearGradient id="qualityGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" style="stop-color:#667eea;stop-opacity:0.3" />
                <stop offset="100%" style="stop-color:#667eea;stop-opacity:0" />
              </linearGradient>
            </defs>
            <polyline fill="url(#qualityGradient)" stroke="#667eea" stroke-width="2"
                      points="0,150 100,120 200,130 300,110 400,100 500,95 600,90"/>
            <circle cx="600" cy="90" r="4" fill="#667eea"/>
          </svg>
        </div>
        <div class="chart-legend">
          <span>Data Quality Score over time</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const selectedTimeframe = ref('month')
const selectedDataset = ref('all')
</script>

<style scoped>
.reports {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.reports-header {
  margin-bottom: 2rem;
}

.reports-header h1 {
  color: #1f2937;
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 700;
}

.reports-header p {
  color: #6b7280;
  margin: 0;
  font-size: 1.125rem;
}

.reports-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.filter-section {
  display: flex;
  gap: 1rem;
}

.filter-select {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #374151;
  font-size: 0.875rem;
}

.generate-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.generate-btn:hover {
  opacity: 0.9;
}

.reports-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-bottom: 3rem;
}

.report-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.report-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 1.125rem;
  font-weight: 600;
}

.report-date {
  color: #6b7280;
  font-size: 0.875rem;
}

.report-stats {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  color: #6b7280;
  font-size: 0.875rem;
}

.stat-value {
  font-weight: 600;
  font-size: 1rem;
}

.stat-value.good {
  color: #059669;
}

.stat-value.warning {
  color: #d97706;
}

.stat-value.error {
  color: #dc2626;
}

.report-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-secondary {
  flex: 1;
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-secondary:hover {
  background: #f9fafb;
}

.btn-primary {
  flex: 1;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.9;
}

.trends-section {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.trends-section h2 {
  margin: 0 0 1.5rem 0;
  color: #1f2937;
  font-size: 1.25rem;
  font-weight: 600;
}

.chart-placeholder {
  margin-bottom: 1rem;
}

.chart-legend {
  text-align: center;
  color: #6b7280;
  font-size: 0.875rem;
}
</style>
