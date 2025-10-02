<template>
  <Teleport to="body">
    <div class="notification-container mobile-notification-container">
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
  notifications.splice(0)

  const id = nextId++
  notifications.push({
    id,
    type: 'info',
    duration: 2000, // Shortened from 5000ms to 3000ms (3 seconds)
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
  top: 1rem;
  right: 1rem;
  z-index: 1050;
  max-width: 400px;
  width: 100%;
}

/* Mobile responsive notifications */
@media (max-width: 768px) {
  .mobile-notification-container {
    top: 80px; /* Position below navbar on mobile */
    right: 0.5rem;
    left: 0.5rem;
    max-width: none;
  }
}

@media (max-width: 576px) {
  .mobile-notification-container {
    top: 80px; /* Keep below navbar */
    right: 0.25rem;
    left: 0.25rem;
  }
}
</style>
