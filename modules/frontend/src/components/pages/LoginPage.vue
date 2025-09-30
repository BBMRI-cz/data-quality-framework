<template>
  <div class="app-container">
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm px-4">
      <div class="container-fluid">
        <div class="ms-auto d-flex align-items-center text-white">
          <i class="bi bi-person-fill fs-4 me-2"></i>
        </div>
      </div>
    </nav>

    <header class="title-section">
      <h1>Data Quality Agent</h1>
    </header>

    <main class="main-content d-flex justify-content-center align-items-start pt-5">
      <div class="card shadow-lg p-4 rounded-4" style="max-width: 400px; width: 100%;">
        <h3 class="mb-3 text-center">Login</h3>

        <div v-if="error" class="alert alert-danger py-2" role="alert">
          {{ error }}
        </div>

        <form @submit.prevent="login" class="d-flex flex-column gap-3" novalidate>
          <input v-model="username" type="text" class="form-control" placeholder="Username" autocomplete="username" required />
          <input v-model="password" type="password" class="form-control" placeholder="Password" autocomplete="current-password" required />
          <div class="d-flex justify-content-between align-items-center">
            <button type="submit" class="btn btn-primary" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ loading ? 'Signing in...' : 'Login' }}
            </button>
          </div>
        </form>
      </div>
    </main>

    <footer class="footer bg-secondary text-white text-center">
      © 2025 BBMRI-ERIC
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authenticate } from '../../js/api.js'
import { useUserStore } from '../../stores/userStore.js'

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const route = useRoute()
const router = useRouter()
const { updateDefaultPasswordStatus } = useUserStore()

async function login() {
  error.value = ''
  loading.value = true
  try {
    const loginResult = await authenticate(username.value, password.value)
    updateDefaultPasswordStatus(loginResult.defaultPassword)

    await router.replace((route.query.redirect && String(route.query.redirect)) || '/')
  } catch (e) {
    error.value = e?.message || 'Invalid username or password'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.title-section,
.main-content {
  background-color: #e9ecef;
}

.title-section {
  text-align: center;
  padding: 2rem 1rem;
}

.title-section h1 {
  font-size: 3rem;
  font-weight: bold;
  margin: 0;
}

.main-content {
  flex-grow: 1;
  padding: 2rem 1rem;
}

.footer {
  padding: 1rem 0;
}
</style>