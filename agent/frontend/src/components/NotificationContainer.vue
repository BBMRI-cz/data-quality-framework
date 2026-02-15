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
  import { reactive } from 'vue';
  import NotificationItem from './NotificationItem.vue';

  const notifications = reactive([]);
  let nextId = 1;

  const addNotification = (notification) => {
    notifications.splice(0);

    const id = nextId++;
    notifications.push({
      id,
      type: 'info',
      duration: 2000,
      autoClose: true,
      ...notification,
    });
    return id;
  };

  const removeNotification = (id) => {
    const index = notifications.findIndex((n) => n.id === id);
    if (index > -1) {
      notifications.splice(index, 1);
    }
  };

  const clearAll = () => {
    notifications.splice(0);
  };

  const showSuccess = (title, message = '', options = {}) => {
    return addNotification({ type: 'success', title, message, ...options });
  };

  const showError = (title, message = '', options = {}) => {
    return addNotification({ type: 'error', title, message, ...options });
  };

  const showInfo = (title, message = '', options = {}) => {
    return addNotification({ type: 'info', title, message, ...options });
  };

  const showWarning = (title, message = '', options = {}) => {
    return addNotification({ type: 'warning', title, message, ...options });
  };

  defineExpose({
    addNotification,
    removeNotification,
    clearAll,
    showSuccess,
    showError,
    showInfo,
    showWarning,
  });
</script>

<style scoped>
  .notification-container {
    position: fixed;
    top: var(--spacing-md);
    right: var(--spacing-md);
    z-index: var(--z-modal);
    max-width: 400px;
    width: 100%;
  }

  /* Mobile responsive notifications */
  @media (max-width: 768px) {
    .mobile-notification-container {
      top: 80px;
      right: var(--spacing-sm);
      left: var(--spacing-sm);
      max-width: none;
    }
  }

  @media (max-width: 576px) {
    .mobile-notification-container {
      top: 80px;
      right: var(--spacing-xs);
      left: var(--spacing-xs);
    }
  }
</style>
