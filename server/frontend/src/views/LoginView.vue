<template>
  <div class="container-fluid vh-100 d-flex align-items-center justify-content-center bg-light">
    <div class="login-container">
      <LoginForm />
      <Copyright />
    </div>
  </div>
</template>

<script setup>
import LoginForm from '../components/LoginForm.vue'
import Copyright from '../components/Copyright.vue'
</script>

<style scoped>
.login-container {
  width: 100%;
  max-width: 900px;
  padding: 0 1rem;
}

@media (min-width: 1400px) {
  .login-container {
    max-width: 1000px;
  }
}

@media (max-width: 992px) {
  .login-container {
    max-width: 700px;
  }
}

@media (max-width: 768px) {
  .login-container {
    max-width: 100%;
    padding: 0 0.5rem;
  }
}
</style>
