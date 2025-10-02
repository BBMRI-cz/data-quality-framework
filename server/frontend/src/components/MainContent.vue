<template>
  <main class="main-content">
    <div class="content-container">
      <!-- Main content area - ready for future components -->
      <div class="welcome-message">
        <h2>Welcome to the Data Quality Dashboard</h2>
        <p>This is where the main application content will be displayed.</p>
      </div>
    </div>
  </main>
</template>

<script setup>
// This component will be expanded with actual content in the future
</script>

<style scoped>
.main-content {
  flex: 1;
  padding: 2rem;
  background: #f8fafc;
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-message {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.welcome-message h2 {
  color: #1f2937;
  margin: 0 0 1rem 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.welcome-message p {
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
}
</style>
