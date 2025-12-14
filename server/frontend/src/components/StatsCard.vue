<template>
  <div class="card border-0 shadow-sm h-100">
    <div class="card-body p-4">
      <div class="d-flex justify-content-between align-items-start">
        <div>
          <p class="text-muted mb-1">{{ label }}</p>
          <p class="display-6 fw-bold text-dark mb-0" :title="tooltipText">{{ displayValue }}</p>
        </div>
        <div class="icon-container p-3 rounded" :style="{ backgroundColor: iconBgColor }">
          <i :class="iconClass" :style="{ color: iconColor }" style="font-size: 1.25rem;"></i>
        </div>
      </div>
      <div class="mt-3" v-if="trendText">
        <p class="mb-0 small d-flex align-items-center" :class="trendClass">
          <i :class="trendIconClass" class="me-1" v-if="trendIcon || trendType !== 'neutral'"></i>
          {{ trendText }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  tooltipText: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    required: true,
    default: 'bi bi-graph-up'
  },
  iconColor: {
    type: String,
    default: '#0d6efd' // Bootstrap primary color
  },
  iconBgColor: {
    type: String,
    default: '#cfe2ff' // Bootstrap primary light
  },
  trendText: {
    type: String,
    default: ''
  },
  trendType: {
    type: String,
    default: 'neutral', // 'positive', 'negative', 'neutral'
    validator: (value) => ['positive', 'negative', 'neutral'].includes(value)
  },
  trendIcon: {
    type: String,
    default: ''
  },
  animationDuration: {
    type: Number,
    default: 1000 // milliseconds
  }
})

const displayValue = ref(typeof props.value === 'number' ? 0 : props.value)

const animateValue = (start, end, duration) => {
  if (typeof end !== 'number') {
    displayValue.value = end
    return
  }

  const startTime = Date.now()
  const range = end - start

  const updateValue = () => {
    const currentTime = Date.now()
    const elapsed = currentTime - startTime

    if (elapsed < duration) {
      const progress = elapsed / duration
      // Ease-out function for smoother animation
      const easeOut = 1 - Math.pow(1 - progress, 3)
      displayValue.value = Math.round(start + range * easeOut)
      requestAnimationFrame(updateValue)
    } else {
      displayValue.value = end
    }
  }

  requestAnimationFrame(updateValue)
}

watch(() => props.value, (newValue, oldValue) => {
  if (typeof newValue === 'number' && typeof oldValue === 'number') {
    animateValue(oldValue, newValue, props.animationDuration)
  } else if (typeof newValue === 'number') {
    animateValue(0, newValue, props.animationDuration)
  } else {
    displayValue.value = newValue
  }
}, { immediate: true })

const iconClass = computed(() => props.icon)

const trendClass = computed(() => {
  switch (props.trendType) {
    case 'positive':
      return 'text-success'
    case 'negative':
      return 'text-danger'
    default:
      return 'text-muted'
  }
})

const trendIconClass = computed(() => {
  if (props.trendIcon) {
    return props.trendIcon
  }

  switch (props.trendType) {
    case 'positive':
      return 'bi bi-arrow-up'
    case 'negative':
      return 'bi bi-arrow-down'
    default:
      return ''
  }
})
</script>

<style scoped>
.icon-container {
  min-width: 48px;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15) !important;
}
</style>
