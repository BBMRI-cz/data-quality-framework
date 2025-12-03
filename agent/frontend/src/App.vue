<template>
  <div id="app">
    <Sidebar v-if="isAuth" />
    <TopNavbar v-if="isAuth" />

    <main class="main-content" :class="{ 'no-sidebar': !isAuth }">
      <router-view v-if="!errorStore.showErrorPage.value" />
      <NotFound v-else />
    </main>

    <NotificationContainer ref="notificationContainer" />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import TopNavbar from './components/TopNavbar.vue'
import NotificationContainer from './components/NotificationContainer.vue'
import NotFound from './views/NotFound.vue'
import { isAuthenticated } from './js/api.js'
import { notificationService } from './services/notificationService.js'
import { useErrorStore } from './stores/errorStore.js'

const router = useRouter()
const route = useRoute()
const notificationContainer = ref(null)
const isAuth = ref(isAuthenticated())
const errorStore = useErrorStore()

// Watch for route changes to update auth status
watch(() => route.path, () => {
  isAuth.value = isAuthenticated()
  // Clear error page when navigating
  errorStore.hideError()
})

onMounted(() => {
  if (notificationContainer.value) {
    notificationService.setContainer(notificationContainer.value)
  }
})
</script>

<style>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.main-content {
  flex: 1;
  background: var(--bg-page);
  margin-left: var(--sidebar-width);
  margin-top: var(--navbar-height);
  min-height: calc(100vh - var(--navbar-height));
  transition: margin-left var(--transition-smooth);
  width: calc(100% - var(--sidebar-width));
}

.main-content.no-sidebar {
  margin-left: 0;
  margin-top: 0;
  min-height: 100vh;
  width: 100%;
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
    margin-top: var(--navbar-height-mobile);
    padding: var(--spacing-md) var(--spacing-sm);
    min-height: calc(100vh - var(--navbar-height-mobile));
    width: 100%;
  }

  .main-content.no-sidebar {
    margin-top: 0;
    min-height: 100vh;
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
    font-size: 16px;
    min-height: 44px;
  }
}
</style>
