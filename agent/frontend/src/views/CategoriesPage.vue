<template>
  <div class="categories-page">
    <PageHeader
      title="Categories"
      mobile-title="Categories"
      subtitle="Manage quality check categories"
      icon="bi bi-tags"
    >
      <template #actions>
        <ActionButton to="/categories/new" icon="bi bi-plus" text="New Category" />
      </template>
    </PageHeader>

    <div class="page-content">
      <div class="stats-grid">
        <StatCard
          :number="categories.length"
          label="Total Categories"
          number-class="text-primary"
          help-text="Number of configured quality check categories"
        />
      </div>

      <div class="search-bar">
        <i class="bi bi-search search-icon"></i>
        <input
          v-model="searchQuery"
          type="text"
          class="form-control"
          placeholder="Search categories..."
        />
        <button
          v-if="searchQuery"
          class="btn btn-link clear-btn"
          type="button"
          @click="searchQuery = ''"
        >
          <i class="bi bi-x-circle"></i>
        </button>
      </div>

      <BaseTable
        title="Category Definitions"
        :columns="columns"
        :items="filteredCategories"
        :loading="loading"
        item-key="id"
        item-label="categories"
        empty-text="No categories configured yet"
        empty-icon="bi bi-tags"
        @row-click="navigateToEdit"
      >
        <template #name="{ item }">
          <span class="fw-medium">{{ item.name }}</span>
        </template>
        <template #color="{ item }">
          <CategoryBadge :category="item" />
        </template>
      </BaseTable>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import StatCard from '@/components/StatCard.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import CategoryBadge from '@/components/CategoryBadge.vue';
  import { categoryService } from '@/services/categoryService.js';
  import { notificationService } from '@/services/notificationService.js';

  const router = useRouter();
  const categories = ref([]);
  const loading = ref(false);
  const searchQuery = ref('');

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'color', label: 'Color' },
  ];

  const filteredCategories = computed(() => {
    if (!searchQuery.value) {
      return categories.value;
    }
    const query = searchQuery.value.toLowerCase();
    return categories.value.filter((category) => category.name?.toLowerCase().includes(query));
  });

  const loadCategories = async () => {
    loading.value = true;
    try {
      categories.value = await categoryService.getAll();
    } catch (error) {
      console.error('Failed to load categories:', error);
      notificationService.error('Load Failed', 'Unable to load categories. Please try again.');
      categories.value = [];
    } finally {
      loading.value = false;
    }
  };

  const navigateToEdit = (category) => {
    router.push(`/categories/${category.id}/edit`);
  };

  onMounted(loadCategories);
</script>

<style scoped>
  .categories-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: var(--spacing-md);
    margin-bottom: var(--spacing-lg);
  }

  .search-bar {
    position: relative;
    margin-bottom: var(--spacing-lg);
    max-width: 400px;
  }

  .search-bar .form-control {
    padding-left: 2.5rem;
    padding-right: 2.5rem;
    border: 2px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    background: var(--bg-card);
  }

  .search-bar .form-control:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  }

  .search-icon {
    position: absolute;
    left: 0.875rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-gray-400);
    font-size: 1rem;
    pointer-events: none;
  }

  .clear-btn {
    position: absolute;
    right: 0.25rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-gray-400);
    padding: 0.25rem 0.5rem;
    text-decoration: none;
  }

  .clear-btn:hover {
    color: var(--color-gray-600);
  }

  @media (max-width: 768px) {
    .categories-page {
      padding: var(--spacing-md);
    }

    .search-bar {
      max-width: 100%;
    }
  }

  @media (max-width: 576px) {
    .categories-page {
      padding: var(--spacing-sm);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
