<template>
  <Teleport to="body">
    <div class="notification-container">
      <NotificationItem
        v-for="notification in notifications"
        :key="notification.id"
        :type="notification.type"
        :title="notification.title"
        :message="notification.message"
        :duration="notification.duration"
        :auto-close="notification.autoClose"
        @close="removeNotification(notification.id)"
      />
    </div>
  </Teleport>
</template>

<script setup>
import { reactive } from 'vue'
import NotificationItem from './NotificationItem.vue'

const notifications = reactive([])
let nextId = 1

const addNotification = (notification) => {
  const id = nextId++
  notifications.push({
    id,
    type: 'info',
    duration: 5000,
    autoClose: true,
    ...notification
  })
  return id
}

const removeNotification = (id) => {
  const index = notifications.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.splice(index, 1)
  }
}

const clearAll = () => {
  notifications.splice(0)
}

// Global notification methods
const showSuccess = (title, message = '', options = {}) => {
  return addNotification({ type: 'success', title, message, ...options })
}

const showError = (title, message = '', options = {}) => {
  return addNotification({ type: 'error', title, message, ...options })
}

const showInfo = (title, message = '', options = {}) => {
  return addNotification({ type: 'info', title, message, ...options })
}

const showWarning = (title, message = '', options = {}) => {
  return addNotification({ type: 'warning', title, message, ...options })
}

// Export methods for global use
defineExpose({
  addNotification,
  removeNotification,
  clearAll,
  showSuccess,
  showError,
  showInfo,
  showWarning
})
</script>

<style scoped>
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  pointer-events: none;
}

.notification-container > * {
  pointer-events: auto;
}
</style>
