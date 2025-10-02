import { ref } from 'vue'

// Global notification service
class NotificationService {
  constructor() {
    this.container = null
  }

  // Set the notification container reference
  setContainer(container) {
    this.container = container
  }

  // Show success notification
  success(title, message = '', options = {}) {
    if (this.container) {
      return this.container.showSuccess(title, message, options)
    }
  }

  // Show error notification
  error(title, message = '', options = {}) {
    if (this.container) {
      return this.container.showError(title, message, options)
    }
  }

  // Show info notification
  info(title, message = '', options = {}) {
    if (this.container) {
      return this.container.showInfo(title, message, options)
    }
  }

  // Show warning notification
  warning(title, message = '', options = {}) {
    if (this.container) {
      return this.container.showWarning(title, message, options)
    }
  }

  // Clear all notifications
  clearAll() {
    if (this.container) {
      this.container.clearAll()
    }
  }
}

export const notificationService = new NotificationService()
