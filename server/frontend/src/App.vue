<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from './components/Navbar.vue'
import AppFooter from './components/AppFooter.vue'
import NotificationContainer from './components/NotificationContainer.vue'
import { authStore } from './stores/authStore.js'
import { notificationService } from './services/notificationService.js'

const router = useRouter()
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

    <AppFooter v-if="!authStore.isAuthenticated" />
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
  background: #f8fafc;
}

.main-content:has(.login-form) {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
