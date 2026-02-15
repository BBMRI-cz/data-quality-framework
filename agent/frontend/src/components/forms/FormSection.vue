<template>
  <div class="form-section">
    <h6 v-if="title" class="section-title">
      <i v-if="icon" :class="['bi', icon, 'me-2']"></i>
      {{ title }}
    </h6>
    <p v-if="description" class="section-description">
      {{ description }}
    </p>
    <slot />
  </div>
</template>

<script setup>
  defineProps({
    title: {
      type: String,
      default: '',
    },
    icon: {
      type: String,
      default: '',
    },
    description: {
      type: String,
      default: '',
    },
  });
</script>

<style scoped>
  .form-section {
    padding: var(--spacing-2xl);
    border-bottom: 1px solid var(--color-gray-200);
  }

  .form-section:last-of-type {
    border-bottom: none;
  }

  .section-title {
    color: var(--color-gray-600);
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 600;
    padding-bottom: var(--spacing-md);
    margin-bottom: var(--spacing-lg);
    border-bottom: 2px solid var(--color-gray-100);
    display: flex;
    align-items: center;
  }

  .section-title i {
    color: var(--color-primary);
  }

  .section-description {
    font-size: 0.95rem;
    color: var(--color-gray-500);
    margin: 0 0 var(--spacing-lg) 0;
    line-height: 1.5;
  }

  /* Responsive adjustments */
  @media (max-width: 768px) {
    .form-section {
      padding: var(--spacing-lg);
    }
  }

  @media (max-width: 576px) {
    .form-section {
      padding: var(--spacing-md);
    }
  }
</style>

