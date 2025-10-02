<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="dashboard-title">Data Quality Dashboard</h1>
          <p class="dashboard-subtitle">Welcome back, {{ authStore.user?.username }}!</p>
        </div>
        <div class="header-right">
          <div class="user-info">
            <span class="user-name">{{ authStore.user?.username }}</span>
            <span class="user-role">{{ authStore.user?.role || 'User' }}</span>
          </div>
          <button @click="handleLogout" class="logout-button">
            Sign Out
          </button>
        </div>
      </div>
    </header>

    <main class="dashboard-main">
      <div class="dashboard-content">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">📊</div>
            <div class="stat-info">
              <h3>Connected Repositories</h3>
              <p class="stat-number">0</p>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon">📋</div>
            <div class="stat-info">
              <h3>Generated Reports</h3>
              <p class="stat-number">0</p>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon">⚡</div>
            <div class="stat-info">
              <h3>System Status</h3>
              <p class="stat-status online">Online</p>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon">🔄</div>
            <div class="stat-info">
              <h3>Last Updated</h3>
              <p class="stat-time">{{ lastUpdated }}</p>
            </div>
          </div>
        </div>

        <div class="content-grid">
          <div class="content-card">
            <h2>Recent Activity</h2>
            <div class="activity-list">
              <div class="activity-item">
                <div class="activity-icon">ℹ️</div>
                <div class="activity-content">
                  <p>System initialized</p>
                  <span class="activity-time">Just now</span>
                </div>
              </div>
              <div class="activity-item">
                <div class="activity-icon">👤</div>
                <div class="activity-content">
                  <p>User {{ authStore.user?.username }} logged in</p>
                  <span class="activity-time">Just now</span>
                </div>
              </div>
            </div>
          </div>

          <div class="content-card">
            <h2>Quick Actions</h2>
            <div class="actions-grid">
              <button class="action-button">
                <span class="action-icon">🔗</span>
                Connect Repository
              </button>
              <button class="action-button">
                <span class="action-icon">📈</span>
                View Reports
              </button>
              <button class="action-button">
                <span class="action-icon">⚙️</span>
                Settings
              </button>
              <button class="action-button">
                <span class="action-icon">❓</span>
                Help & Support
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { authStore } from '../stores/authStore.js'
import { notificationService } from '../services/notificationService.js'

const lastUpdated = ref('')

const updateTime = () => {
  const now = new Date()
  lastUpdated.value = now.toLocaleTimeString()
}

const handleLogout = () => {
  authStore.logout()
  notificationService.info('Signed Out', 'You have been successfully signed out.')
}

onMounted(() => {
  updateTime()
  // Update time every minute
  setInterval(updateTime, 60000)
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: #f8fafc;
}

.dashboard-header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem 2rem;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 0.25rem 0;
}

.dashboard-subtitle {
  color: #6b7280;
  margin: 0;
  font-size: 0.9rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.user-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 0.9rem;
}

.user-role {
  font-size: 0.8rem;
  color: #6b7280;
  text-transform: capitalize;
}

.logout-button {
  background: #ef4444;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.logout-button:hover {
  background: #dc2626;
}

.dashboard-main {
  padding: 2rem;
}

.dashboard-content {
  max-width: 1200px;
  margin: 0 auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: shadow 0.2s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 2rem;
  flex-shrink: 0;
}

.stat-info h3 {
  font-size: 0.9rem;
  font-weight: 600;
  color: #374151;
  margin: 0 0 0.5rem 0;
}

.stat-number {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.stat-status {
  font-size: 0.9rem;
  font-weight: 600;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  margin: 0;
}

.stat-status.online {
  background: #dcfce7;
  color: #166534;
}

.stat-time {
  font-size: 0.9rem;
  color: #6b7280;
  margin: 0;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}

.content-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.content-card h2 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 1rem 0;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.activity-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.activity-content p {
  font-size: 0.9rem;
  color: #374151;
  margin: 0 0 0.25rem 0;
}

.activity-time {
  font-size: 0.8rem;
  color: #6b7280;
}

.actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.action-button {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  padding: 1rem;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.9rem;
  color: #374151;
}

.action-button:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
  transform: translateY(-1px);
}

.action-icon {
  font-size: 1.5rem;
}

@media (max-width: 768px) {
  .dashboard-header {
    padding: 1rem;
  }

  .header-content {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .dashboard-main {
    padding: 1rem;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .actions-grid {
    grid-template-columns: 1fr;
  }
}
</style>
