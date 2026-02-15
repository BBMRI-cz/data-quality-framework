<template>
  <Teleport to="body">
    <div class="notification-container mobile-notification-container">
      <TransitionGroup name="notification">
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
      </TransitionGroup>
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
      duration: 8000,
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
    top: calc(var(--navbar-height) + var(--spacing-sm));
    right: var(--spacing-md);
    z-index: var(--z-modal);
    max-width: 500px;
    width: 100%;
  }

  /* Transition animations */
  .notification-enter-active {
    transition: all var(--transition-slow);
  }

  .notification-leave-active {
    transition: all var(--transition-base);
  }

  .notification-enter-from {
    opacity: 0;
    transform: translateX(100%);
  }

  .notification-leave-to {
    opacity: 0;
    transform: translateX(100%);
  }

  .notification-move {
    transition: transform var(--transition-base);
  }

  /* Mobile responsive notifications */
  @media (max-width: 768px) {
    .mobile-notification-container {
      top: calc(var(--navbar-height-mobile) + var(--spacing-sm));
      right: var(--spacing-sm);
      left: var(--spacing-sm);
      max-width: none;
    }

    .notification-enter-from,
    .notification-leave-to {
      transform: translateY(-100%);
    }
  }

  @media (max-width: 576px) {
    .mobile-notification-container {
      top: calc(var(--navbar-height-mobile) + var(--spacing-sm));
      right: var(--spacing-xs);
      left: var(--spacing-xs);
    }
  }
</style>
