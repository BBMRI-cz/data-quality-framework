<template>
  <form @submit.prevent="login" style="display:flex; gap:8px; align-items:center;">
    <input v-model="username" placeholder="username" autocomplete="username" />
    <input v-model="password" type="password" placeholder="password" autocomplete="current-password" />
    <button type="submit">Login</button>
    <button type="button" @click="logout">Logout</button>
  </form>
</template>

<script setup>
import { ref } from 'vue';
import { setAuth, clearAuth } from '../js/api';

const username = ref('');
const password = ref('');

function login() {
  setAuth(username.value, password.value);
}

function logout() {
  clearAuth();
}
</script>