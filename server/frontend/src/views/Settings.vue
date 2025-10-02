<template>
  <div class="settings">
    <div class="settings-header">
      <h1>Settings</h1>
      <p>Configure your data quality monitoring preferences</p>
    </div>

    <div class="settings-sections">
      <div class="settings-section">
        <h2>Data Sources</h2>
        <div class="setting-item">
          <div class="setting-info">
            <h3>Database Connection</h3>
            <p>Configure your primary database connection</p>
          </div>
          <button class="setting-action">Configure</button>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <h3>File Upload Settings</h3>
            <p>Set file format preferences and validation rules</p>
          </div>
          <button class="setting-action">Configure</button>
        </div>
      </div>

      <div class="settings-section">
        <h2>Quality Rules</h2>
        <div class="setting-item">
          <div class="setting-info">
            <h3>Validation Thresholds</h3>
            <p>Set minimum quality scores and alert thresholds</p>
          </div>
          <div class="threshold-controls">
            <label>
              Quality Score Threshold:
              <input type="range" v-model="qualityThreshold" min="0" max="100" />
              <span>{{ qualityThreshold }}%</span>
            </label>
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <h3>Custom Rules</h3>
            <p>Create and manage custom data validation rules</p>
          </div>
          <button class="setting-action">Manage Rules</button>
        </div>
      </div>

      <div class="settings-section">
        <h2>Notifications</h2>
        <div class="setting-item">
          <div class="setting-info">
            <h3>Email Alerts</h3>
            <p>Receive email notifications for quality issues</p>
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="emailAlerts" />
            <span class="slider"></span>
          </label>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <h3>Report Schedule</h3>
            <p>Automatically generate and send reports</p>
          </div>
          <select v-model="reportFrequency" class="setting-select">
            <option value="daily">Daily</option>
            <option value="weekly">Weekly</option>
            <option value="monthly">Monthly</option>
            <option value="never">Never</option>
          </select>
        </div>
      </div>

      <div class="settings-section">
        <h2>Account</h2>
        <div class="setting-item">
          <div class="setting-info">
            <h3>Profile Information</h3>
            <p>Update your account details</p>
          </div>
          <button class="setting-action">Edit Profile</button>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <h3>Change Password</h3>
            <p>Update your account password</p>
          </div>
          <button class="setting-action">Change Password</button>
        </div>
      </div>
    </div>

    <div class="settings-actions">
      <button class="btn-secondary" @click="resetSettings">Reset to Defaults</button>
      <button class="btn-primary" @click="saveSettings">Save Changes</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { notificationService } from '../services/notificationService.js'

const qualityThreshold = ref(85)
const emailAlerts = ref(true)
const reportFrequency = ref('weekly')

const saveSettings = () => {
  // Save settings logic would go here
  notificationService.success('Settings Saved', 'Your preferences have been updated successfully.')
}

const resetSettings = () => {
  qualityThreshold.value = 85
  emailAlerts.value = true
  reportFrequency.value = 'weekly'
  notificationService.info('Settings Reset', 'Settings have been reset to default values.')
}
</script>

<style scoped>
.settings {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.settings-header {
  margin-bottom: 2rem;
}

.settings-header h1 {
  color: #1f2937;
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 700;
}

.settings-header p {
  color: #6b7280;
  margin: 0;
  font-size: 1.125rem;
}

.settings-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  margin-bottom: 2rem;
}

.settings-section {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.settings-section h2 {
  margin: 0 0 1.5rem 0;
  color: #1f2937;
  font-size: 1.25rem;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.75rem;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #f3f4f6;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info h3 {
  margin: 0 0 0.25rem 0;
  color: #1f2937;
  font-size: 1rem;
  font-weight: 500;
}

.setting-info p {
  margin: 0;
  color: #6b7280;
  font-size: 0.875rem;
}

.setting-action {
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: opacity 0.2s;
}

.setting-action:hover {
  opacity: 0.9;
}

.setting-select {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #374151;
  font-size: 0.875rem;
}

.threshold-controls {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.threshold-controls label {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: #374151;
  font-size: 0.875rem;
}

.threshold-controls input[type="range"] {
  flex: 1;
  max-width: 200px;
}

.threshold-controls span {
  font-weight: 600;
  min-width: 40px;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 60px;
  height: 34px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 34px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 26px;
  width: 26px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

input:checked + .slider:before {
  transform: translateX(26px);
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-secondary:hover {
  background: #f9fafb;
}

.btn-primary {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.9;
}
</style>
