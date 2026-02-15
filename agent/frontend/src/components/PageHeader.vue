<template>
  <div class="page-header mb-3 mb-md-4">
    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-start gap-2">
      <div class="header-content">
        <h1 class="page-title mb-1">
          <i v-if="icon" :class="['me-2', 'text-primary', icon]"></i>
          <span :class="{ 'd-none d-sm-inline': mobileTitle }">{{ title }}</span>
          <span v-if="mobileTitle" class="d-inline d-sm-none">{{ mobileTitle }}</span>
        </h1>
        <p
          v-if="subtitle"
          class="page-subtitle"
          :class="{ 'd-none d-sm-block': hideSubtitleOnMobile }"
        >
          {{ subtitle }}
        </p>
      </div>
      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
  defineProps({
    title: {
      type: String,
      required: true,
    },
    subtitle: {
      type: String,
      default: '',
    },
    icon: {
      type: String,
      default: '',
    },
    mobileTitle: {
      type: String,
      default: '',
    },
    hideSubtitleOnMobile: {
      type: Boolean,
      default: true,
    },
  });
</script>

<style scoped>
  .page-header {
    padding-bottom: var(--spacing-md);
  }

  .page-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--color-gray-800);
    margin: 0;
  }

  .page-subtitle {
    color: var(--color-gray-500);
    font-size: 0.875rem;
    margin: 0;
  }

  .header-actions {
    flex-shrink: 0;
  }

  @media (min-width: 768px) {
    .page-title {
      font-size: 1.75rem;
    }

    .page-subtitle {
      font-size: 0.95rem;
    }
  }
</style>
