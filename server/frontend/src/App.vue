<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import TopNavbar from './components/TopNavbar.vue'
import AppFooter from './components/AppFooter.vue'
import NotificationContainer from './components/NotificationContainer.vue'
import CookieConsent from './components/CookieConsent.vue'
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
    <Sidebar v-if="authStore.isAuthenticated" />
    <TopNavbar v-if="authStore.isAuthenticated" />
    <main :class="['main-content', { 'no-sidebar': !authStore.isAuthenticated }]">
      <router-view />
    </main>
    <NotificationContainer ref="notificationContainer" />
    <CookieConsent />
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
  margin-left: 260px;
  margin-top: 64px;
  min-height: calc(100vh - 64px);
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  width: calc(100% - 260px);
}

/* When sidebar is collapsed */
.main-content.sidebar-collapsed {
  margin-left: 80px;
  width: calc(100% - 80px);
}

/* When not authenticated (login page) */
.main-content.no-sidebar {
  margin-left: 0;
  margin-top: 0;
  min-height: auto;
  width: 100%;
  padding: 1.5rem 0 2rem; /* reduced vertical padding */
  display: block; /* prevent flex child full-height expansion */
  flex: 0; /* override flex:1 from base */
}

/* Mobile-specific styles */
@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
    margin-top: 56px;
    padding: 0;
    min-height: calc(100vh - 56px);
    width: 100%;
  }

  .main-content.sidebar-collapsed {
    margin-left: 0;
  }

  .main-content.no-sidebar {
    margin-top: 0;
    min-height: auto; /* allow natural height on mobile */
    width: 100%;
    padding: 1.25rem 0 1.75rem; /* smaller on mobile */
    flex: 0;
    display: block;
  }

  .btn {
    min-height: 44px;
    padding: 0.75rem 1rem;
  }

  .btn-sm {
    min-height: 36px;
    padding: 0.5rem 0.75rem;
  }

  .form-control,
  .form-select {
    font-size: 16px; /* Prevent zoom on iOS */
    min-height: 44px;
  }
}
</style>
