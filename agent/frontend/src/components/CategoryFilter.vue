<template>
  <div class="category-filter">
    <button
      class="filter-chip"
      :class="{ active: selectedCategory === null }"
      @click="selectCategory(null)"
    >
      All
    </button>
    <button
      v-for="category in categories"
      :key="category.id"
      class="filter-chip"
      :class="{ active: isActiveCategory(category) }"
      :style="getChipStyle(category)"
      @click="selectCategory(category.name)"
    >
      {{ category.name }}
    </button>
    <button
      class="filter-chip"
      :class="{ active: isNoCategory(selectedCategory) }"
      :style="getNoCategoryChipStyle()"
      @click="selectCategory(NO_CATEGORY_VALUE)"
    >
      No Category
    </button>
  </div>
</template>

<script setup>
  import { computed } from 'vue';

  const NO_CATEGORY_VALUE = 'none';

  const props = defineProps({
    categories: {
      type: Array,
      required: true,
    },
    modelValue: {
      type: String,
      default: null,
    },
  });

  const emit = defineEmits(['update:modelValue']);

  const selectedCategory = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value),
  });

  const selectCategory = (categoryName) => {
    selectedCategory.value = categoryName;
  };

  const isNoCategory = (value) => value === NO_CATEGORY_VALUE;

  const isActiveCategory = (category) => selectedCategory.value === category.name;

  const getChipStyle = (category) => {
    const isActive = isActiveCategory(category);
    const color = category.colorHex || '#6b7280';

    if (isActive) {
      return {
        backgroundColor: color,
        borderColor: color,
        color: getContrastColor(color),
      };
    }

    return {
      backgroundColor: `${color}15`,
      borderColor: `${color}40`,
      color: color,
    };
  };

  const getNoCategoryChipStyle = () => {
    const isActive = isNoCategory(selectedCategory.value);
    const color = '#6b7280';

    if (isActive) {
      return {
        backgroundColor: color,
        borderColor: color,
        color: '#ffffff',
      };
    }

    return {
      backgroundColor: `${color}15`,
      borderColor: `${color}40`,
      color: color,
    };
  };

  const getContrastColor = (hexColor) => {
    const hex = hexColor.replace('#', '');
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.6 ? '#1f2937' : '#ffffff';
  };
</script>

<style scoped>
  .category-filter {
    display: flex;
    flex-wrap: wrap;
    gap: var(--spacing-sm);
    align-items: center;
  }

  .filter-chip {
    padding: 0.375rem 0.875rem;
    border-radius: var(--radius-full);
    border: 1px solid var(--color-gray-300);
    background: var(--bg-card);
    color: var(--color-gray-600);
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    transition: all var(--transition-base);
    line-height: 1.4;
  }

  .filter-chip:hover {
    transform: translateY(-1px);
    box-shadow: var(--shadow-sm);
  }

  .filter-chip.active {
    border-color: var(--color-primary);
    background: var(--color-primary);
    color: white;
    font-weight: 600;
  }
</style>
