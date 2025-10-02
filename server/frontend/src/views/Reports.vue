<template>
  <div class="container-fluid py-3 py-md-4">
    <div class="row justify-content-center">
      <div class="col-12 col-lg-8">
        <div class="card border-0 shadow-sm mobile-reports-card">
          <div class="card-body text-center py-4 py-md-5">
            <div class="mb-3 mb-md-4">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="text-primary reports-icon">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" fill="currentColor"/>
              </svg>
            </div>
            <h2 class="h4 h-md-3 fw-bold text-dark mb-2 mb-md-3">Reports</h2>
            <p class="text-muted mb-0 reports-description">This is the reports page - detailed analytics and reports will be implemented here.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.mobile-reports-card {
  border-radius: 12px;
}

.reports-icon {
  width: 48px;
  height: 48px;
}

.reports-description {
  font-size: 0.95rem;
  max-width: 400px;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .reports-icon {
    width: 40px;
    height: 40px;
  }

  .reports-description {
    font-size: 0.9rem;
    line-height: 1.5;
  }

  .mobile-reports-card {
    margin: 0 0.5rem;
  }
}

@media (max-width: 576px) {
  .container-fluid {
    padding-left: 0.75rem;
    padding-right: 0.75rem;
  }

  .card-body {
    padding: 2rem 1.5rem !important;
  }
}
</style>
