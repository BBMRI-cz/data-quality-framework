<template>
  <div
    class="toast align-items-center border-0 show mobile-toast"
    :class="toastClass"
    role="alert"
    aria-live="assertive"
    aria-atomic="true"
  >
    <div class="d-flex">
      <div class="toast-body d-flex align-items-center">
        <div class="me-2 notification-icon">
          <svg v-if="type === 'success'" width="16" height="16" fill="currentColor" class="text-success" viewBox="0 0 16 16">
            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.061L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
          </svg>
          <svg v-else-if="type === 'error'" width="16" height="16" fill="currentColor" class="text-danger" viewBox="0 0 16 16">
            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM5.354 4.646a.5.5 0 1 0-.708.708L7.293 8l-2.647 2.646a.5.5 0 0 0 .708.708L8 8.707l2.646 2.647a.5.5 0 0 0 .708-.708L8.707 8l2.647-2.646a.5.5 0 0 0-.708-.708L8 7.293 5.354 4.646z"/>
          </svg>
          <svg v-else-if="type === 'warning'" width="16" height="16" fill="currentColor" class="text-warning" viewBox="0 0 16 16">
            <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
          </svg>
          <svg v-else width="16" height="16" fill="currentColor" class="text-info" viewBox="0 0 16 16">
            <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2z"/>
          </svg>
        </div>
        <div class="flex-grow-1">
          <div class="fw-semibold notification-title">{{ title }}</div>
          <div v-if="message" class="small opacity-75 notification-message">{{ message }}</div>
        </div>
      </div>
      <button type="button" class="btn-close me-2 m-auto mobile-close-btn" @click="closeNotification" aria-label="Close"></button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['success', 'error', 'warning', 'info'].includes(value)
  },
  title: {
    type: String,
    required: true
  },
  message: {
    type: String,
    default: ''
  },
  duration: {
    type: Number,
    default: 5000
  },
  autoClose: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['close'])

const toastClass = computed(() => {
  const baseClass = 'text-bg-'
  switch (props.type) {
    case 'success':
      return baseClass + 'success'
    case 'error':
      return baseClass + 'danger'
    case 'warning':
      return baseClass + 'warning'
    default:
      return baseClass + 'info'
  }
})

let timeoutId = null

const closeNotification = () => {
  emit('close')
}

onMounted(() => {
  if (props.autoClose && props.duration > 0) {
    timeoutId = setTimeout(() => {
      closeNotification()
    }, props.duration)
  }
})

onUnmounted(() => {
  if (timeoutId) {
    clearTimeout(timeoutId)
  }
})
</script>

<style scoped>
.mobile-toast {
  margin-bottom: 0.5rem;
  box-shadow: 0 0.25rem 0.75rem rgba(0, 0, 0, 0.1);
  border-radius: 0.5rem;
}

.notification-icon {
  flex-shrink: 0;
}

.notification-title {
  font-size: 0.9rem;
  line-height: 1.3;
}

.notification-message {
  font-size: 0.8rem;
  line-height: 1.4;
}

.mobile-close-btn {
  min-width: 24px;
  min-height: 24px;
}

/* Mobile-specific adjustments */
@media (max-width: 768px) {
  .mobile-toast {
    margin-bottom: 0.75rem;
    font-size: 0.9rem;
  }

  .toast-body {
    padding: 0.75rem 1rem;
  }

  .notification-title {
    font-size: 0.95rem;
  }

  .notification-message {
    font-size: 0.85rem;
    margin-top: 0.25rem;
  }

  .mobile-close-btn {
    min-width: 32px;
    min-height: 32px;
    margin-right: 0.5rem !important;
  }
}

@media (max-width: 576px) {
  .mobile-toast {
    border-radius: 0.75rem;
  }

  .toast-body {
    padding: 1rem;
  }
}
</style>
