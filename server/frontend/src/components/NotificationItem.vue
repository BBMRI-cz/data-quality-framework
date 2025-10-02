<template>
  <Transition name="notification">
    <div
      v-if="visible"
      :class="['notification', `notification--${type}`]"
      @click="close"
    >
      <div class="notification__icon">
        <span v-if="type === 'success'">✓</span>
        <span v-else-if="type === 'error'">✕</span>
        <span v-else>ℹ</span>
      </div>
      <div class="notification__content">
        <div class="notification__title">{{ title }}</div>
        <div v-if="message" class="notification__message">{{ message }}</div>
      </div>
      <button class="notification__close" @click="close" aria-label="Close notification">
        ×
      </button>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['success', 'error', 'info', 'warning'].includes(value)
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
    default: 5000 // 5 seconds
  },
  autoClose: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['close'])

const visible = ref(true)

const close = () => {
  visible.value = false
  setTimeout(() => {
    emit('close')
  }, 300) // Wait for transition to complete
}

onMounted(() => {
  if (props.autoClose && props.duration > 0) {
    setTimeout(() => {
      close()
    }, props.duration)
  }
})
</script>

<style scoped>
.notification {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  max-width: 400px;
  background: white;
  border-left: 4px solid;
}

.notification:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.notification--success {
  border-left-color: #10b981;
  background: #f0fdf4;
}

.notification--error {
  border-left-color: #ef4444;
  background: #fef2f2;
}

.notification--info {
  border-left-color: #3b82f6;
  background: #eff6ff;
}

.notification--warning {
  border-left-color: #f59e0b;
  background: #fffbeb;
}

.notification__icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 12px;
  margin-right: 12px;
  margin-top: 2px;
}

.notification--success .notification__icon {
  background: #10b981;
  color: white;
}

.notification--error .notification__icon {
  background: #ef4444;
  color: white;
}

.notification--info .notification__icon {
  background: #3b82f6;
  color: white;
}

.notification--warning .notification__icon {
  background: #f59e0b;
  color: white;
}

.notification__content {
  flex: 1;
}

.notification__title {
  font-weight: 600;
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 4px;
}

.notification--success .notification__title {
  color: #065f46;
}

.notification--error .notification__title {
  color: #991b1b;
}

.notification--info .notification__title {
  color: #1e40af;
}

.notification--warning .notification__title {
  color: #92400e;
}

.notification__message {
  font-size: 13px;
  line-height: 1.4;
  opacity: 0.8;
}

.notification__close {
  flex-shrink: 0;
  background: none;
  border: none;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  opacity: 0.5;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
  border-radius: 50%;
  transition: opacity 0.2s ease;
}

.notification__close:hover {
  opacity: 1;
}

/* Transition animations */
.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s ease;
}

.notification-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
