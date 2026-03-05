<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <h1 class="error-title">Page Not Found</h1>
      <p class="error-message">
        Oops! The page you're looking for doesn't exist or has been moved.
      </p>
      <div class="error-illustration">
        <svg width="300" height="225" viewBox="0 0 200 150" xmlns="http://www.w3.org/2000/svg">
          <circle cx="100" cy="75" r="60" fill="#f8f9fa" stroke="#dee2e6" stroke-width="2" />
          <circle cx="85" cy="65" r="8" fill="#6c757d" />
          <circle cx="115" cy="65" r="8" fill="#6c757d" />
          <path
            d="M75 90 Q100 105 125 90"
            stroke="#6c757d"
            stroke-width="3"
            fill="none"
            stroke-linecap="round"
          />
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
  // Script content removed since buttons are no longer needed
</script>

<style scoped>
  .not-found-container {
    min-height: calc(100vh - 64px);
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
    padding: 4rem 2rem;
  }

  .not-found-content {
    text-align: center;
    max-width: 800px;
    width: 100%;
  }

  .error-code {
    font-size: 8rem;
    font-weight: 700;
    color: #dc3545;
    margin-bottom: 1.5rem;
    text-shadow: 4px 4px 8px rgba(220, 53, 69, 0.3);
    line-height: 1;
  }

  .error-title {
    font-size: 3rem;
    font-weight: 700;
    color: #343a40;
    margin-bottom: 1.5rem;
    line-height: 1.2;
  }

  .error-message {
    font-size: 1.25rem;
    color: #6c757d;
    margin-bottom: 2rem;
    line-height: 1.6;
    max-width: 600px;
    margin-left: auto;
    margin-right: auto;
  }

  .error-illustration {
    margin-top: 2rem;
    opacity: 0.8;
  }

  .error-illustration svg {
    max-width: 100%;
    height: auto;
  }

  /* Mobile responsive */
  @media (max-width: 768px) {
    .not-found-container {
      padding: 2rem 1rem;
      min-height: calc(100vh - 56px);
    }

    .not-found-content {
      max-width: 100%;
    }

    .error-code {
      font-size: 5rem;
    }

    .error-title {
      font-size: 2rem;
    }

    .error-message {
      font-size: 1.1rem;
    }

    .error-illustration svg {
      width: 200px;
    }
  }
</style>
