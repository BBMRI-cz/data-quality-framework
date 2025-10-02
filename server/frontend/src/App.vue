<script setup>
import { onMounted, ref } from 'vue'
import LoginForm from './components/LoginForm.vue'
import Dashboard from './components/Dashboard.vue'
import AppFooter from './components/AppFooter.vue'
import NotificationContainer from './components/NotificationContainer.vue'
import { authStore } from './stores/authStore.js'
import { notificationService } from './services/notificationService.js'

const notificationContainer = ref(null)

onMounted(() => {
  // Initialize the notification service with the container
  if (notificationContainer.value) {
    notificationService.setContainer(notificationContainer.value)
  }
})
</script>

<template>
  <div id="app">
    <main class="main-content">
      <!-- Show dashboard if authenticated, otherwise show login form -->
      <Dashboard v-if="authStore.isAuthenticated" />
      <LoginForm v-else />
    </main>

    <!-- Footer only shown on login page -->
    <AppFooter v-if="!authStore.isAuthenticated" />

    <!-- Global notification system -->
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
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* When dashboard is shown, main-content should not center */
.main-content:has(.dashboard) {
  align-items: stretch;
  justify-content: stretch;
}
</style>
