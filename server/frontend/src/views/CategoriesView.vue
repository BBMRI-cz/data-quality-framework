<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Categories"
      mobile-title="Categories"
      subtitle="Manage quality check categories"
      icon="bi bi-tags"
    >
      <template #actions>
        <button
          class="btn btn-outline-primary btn-sm"
          :disabled="loading"
          @click="refreshCategories"
        >
          <i class="bi bi-arrow-clockwise"></i>
          <span class="d-none d-md-inline ms-1">Refresh</span>
        </button>
        <button class="btn btn-primary btn-sm ms-2" :disabled="loading" @click="createCategory">
          <i class="bi bi-plus-lg"></i>
          <span class="d-none d-md-inline ms-1">New Category</span>
        </button>
      </template>
    </PageHeader>

    <div class="row g-3 mb-3 mb-md-4">
      <div class="col-12 col-sm-6 col-lg-4">
        <StatsCard
          label="Total Categories"
          :value="categories.length"
          icon="bi bi-tags"
          color="var(--color-primary)"
        />
      </div>
    </div>

    <div class="mb-3 mb-md-4">
      <SearchBar v-model="searchQuery" placeholder="Search categories..." />
    </div>

    <PaginatedTable
      title="Category Definitions"
      :columns="tableColumns"
      :items="filteredCategories"
      :total-items="filteredCategories.length"
      :loading="loading"
      :error="error"
      :empty-title="emptyTitle"
      :empty-text="emptyText"
      item-key="id"
      item-label="categories"
      :paginate="false"
      @row-click="viewCategoryDetail"
    >
      <template #header-meta>
        <Badge :text="`${filteredCategories.length} categories`" variant="secondary" size="small" />
      </template>

      <template #cell-name="{ value }">
        <span class="fw-medium">{{ value }}</span>
      </template>

      <template #cell-color="{ item }">
        <ValuesFilterBadge :category="item" />
      </template>
    </PaginatedTable>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { apiService } from '@/services/apiService.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import SearchBar from '@/components/ui/SearchBar.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import Badge from '@/components/ui/Badge.vue';
  import ValuesFilterBadge from '@/components/ui/ValuesFilterBadge.vue';

  const router = useRouter();
  const categories = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');
  const tableColumns = [
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

  const emptyTitle = computed(() => 'No Categories Found');
  const emptyText = computed(() =>
    searchQuery.value ? 'Try adjusting your search criteria' : 'No categories are configured yet'
  );

  const loadCategories = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getCategories();
      // Handle HAL format response
      if (data._embedded && data._embedded.categories) {
        categories.value = data._embedded.categories;
      } else if (Array.isArray(data)) {
        categories.value = data;
      } else {
        categories.value = [];
      }
    } catch (err) {
      error.value = err.message || 'Failed to load categories';
      console.error('Error loading categories:', err);
    } finally {
      loading.value = false;
    }
  };

  const refreshCategories = () => {
    loadCategories();
  };

  const createCategory = () => {
    router.push('/categories/new');
  };

  const viewCategoryDetail = (category) => {
    router.push(`/categories/${category.id}`);
  };

  onMounted(() => {
    loadCategories();
  });
</script>
