<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from './components/Navbar.vue'
import AppFooter from './components/AppFooter.vue'
import NotificationContainer from './components/NotificationContainer.vue'
import { authStore } from './stores/authStore.js'
import { notificationService } from './services/notificationService.js'

useRouter()
const notificationContainer = ref(null)

onMounted(() => {
  if (notificationContainer.value) {
    notificationService.setContainer(notificationContainer.value)
  }
})
</script>

<template>
  <div id="app">
    <Navbar v-if="authStore.isAuthenticated" />

    <main class="main-content">
      <router-view />
    </main>

    <AppFooter/>
    <NotificationContainer ref="notificationContainer" />
  </div>
</template>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #f8fafc;
  min-height: 100vh;
  /* Improve text rendering on mobile */
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  /* Prevent zoom on double tap */
  touch-action: manipulation;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  /* Ensure app takes full width on mobile */
  width: 100%;
}

.main-content {
  flex: 1;
  background: #f8fafc;
  /* Remove padding that constrains the navbar */
  width: 100%;
}

.main-content:has(.login-form) {
  display: flex;
  align-items: center;
  justify-content: center;
  /* Adjust padding for mobile login */
  padding: 1rem;
}

/* Mobile-specific styles */
@media (max-width: 768px) {
  .main-content {
    padding: 0 0.5rem;
  }

  /* Ensure containers don't overflow */
  .container-fluid {
    padding-left: 0.75rem;
    padding-right: 0.75rem;
  }
}

/* Touch-friendly button sizes */
@media (max-width: 768px) {
  .btn {
    min-height: 44px;
    padding: 0.75rem 1rem;
  }

  .btn-sm {
    min-height: 36px;
    padding: 0.5rem 0.75rem;
  }
}

/* Improve form inputs on mobile */
@media (max-width: 768px) {
  .form-control,
  .form-select {
    font-size: 16px; /* Prevent zoom on iOS */
    min-height: 44px;
  }
}
</style>
