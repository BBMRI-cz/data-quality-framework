<template>
  <div class="container-fluid py-3 py-md-4">
    <div class="row justify-content-center">
      <div class="col-12 col-lg-8">
        <div class="card border-0 shadow-sm mobile-dashboard-card">
          <div class="card-body text-center py-4 py-md-5">
            <div class="mb-3 mb-md-4">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="text-primary dashboard-icon">
                <path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z" fill="currentColor"/>
              </svg>
            </div>
            <h2 class="h4 h-md-3 fw-bold text-dark mb-2 mb-md-3">Dashboard</h2>
            <p class="text-muted mb-0 dashboard-description">This is the main dashboard page - content will be implemented here.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.mobile-dashboard-card {
  border-radius: 12px;
}

.dashboard-icon {
  width: 48px;
  height: 48px;
}

.dashboard-description {
  font-size: 0.95rem;
  max-width: 400px;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .dashboard-icon {
    width: 40px;
    height: 40px;
  }

  .dashboard-description {
    font-size: 0.9rem;
    line-height: 1.5;
  }

  .mobile-dashboard-card {
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
