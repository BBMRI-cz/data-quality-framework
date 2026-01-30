<template>
  <div class="quality-checks-page">
    <PageHeader
      title="Quality Checks"
      mobileTitle="Checks"
      subtitle="View and manage quality checks for your data"
      icon="bi bi-check2-square"
    />

    <div class="page-content">
      <QualityCheckTable />
    </div>
  </div>
</template>

<script setup>
import QualityCheckTable from "@/components/QualityCheckTable.vue";
import PageHeader from "@/components/PageHeader.vue";
</script>

<style scoped>
.quality-checks-page { min-height: 100%; padding: 2rem; }
.page-content { width: 100%; }
@media (max-width: 768px) { .quality-checks-page { padding: 1rem; } }
@media (max-width: 576px) { .quality-checks-page { padding: 0.75rem; } }
</style>
