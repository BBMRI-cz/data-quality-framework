<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          :title="manifest?.name || 'Manifest Details'"
          subtitle="Manifest Versions"
          mobile-title="Versions"
          icon="bi bi-file-earmark-lock"
        >
          <template #actions>
            <button class="btn btn-primary btn-sm" :disabled="loading" @click="goToPublish">
              <i class="bi bi-plus-lg"></i>
              <span class="d-none d-md-inline ms-1">Publish New Version</span>
            </button>
          </template>
        </PageHeader>

        <!-- Back Button -->
        <div class="mb-3">
          <button class="btn btn-outline-secondary btn-sm" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>Back to Manifests
          </button>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading manifest...</span>
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Manifest</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Detail View -->
        <div v-else-if="manifest">
          <!-- Stats -->
          <div class="row g-3 mb-4">
            <div class="col-12 col-sm-6">
              <StatsCard
                label="Published Versions"
                :value="versions.length"
                icon="bi bi-clock-history"
                color="var(--color-primary)"
              />
            </div>
            <div class="col-12 col-sm-6">
              <StatsCard
                label="Latest Published"
                :value="latestGeneratedAt"
                icon="bi bi-calendar-event"
                trend-text="Latest version date"
                trend-type="neutral"
              />
            </div>
          </div>

          <!-- Versions Card -->
          <ManifestVersions :versions="versions" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useHead } from '@unhead/vue';
  import { apiService } from '@/services/apiService.js';
  import { formatDateLong } from '@/utils/dateUtils.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import ManifestVersions from '@/components/ManifestVersions.vue';

  const route = useRoute();
  const router = useRouter();

  const manifestId = ref(route.params.id);
  const manifest = ref(null);
  const versions = ref([]);
  const loading = ref(true);
  const error = ref(null);

  useHead({
    title: computed(() => manifest.value?.name || 'Manifest Versions'),
  });

  const latestGeneratedAt = computed(() => {
    const latest = versions.value[versions.value.length - 1];
    return latest ? formatDateLong(latest.generatedAt) : '—';
  });

  const loadManifest = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getManifest(manifestId.value);
      manifest.value = data;

      if (!manifest.value || !manifest.value.id) {
        error.value = 'Manifest not found';
        return;
      }

      versions.value = manifest.value.versions ? [...manifest.value.versions] : [];
    } catch (err) {
      error.value = err.message || 'Failed to load manifest';
      console.error('Error loading manifest:', err);
    } finally {
      loading.value = false;
    }
  };

  const goToPublish = () => {
    router.push(`/manifests/${manifestId.value}/publish`);
  };

  const goBack = () => {
    router.push('/manifests');
  };

  onMounted(loadManifest);
</script>

<style scoped>
  /* Loading State */
  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 4rem 0;
  }
</style>
