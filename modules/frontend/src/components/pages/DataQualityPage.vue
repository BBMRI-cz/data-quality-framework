<template>
  <div class="app-container">
    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm px-4">
      <div class="container-fluid">
        <div class="ms-auto d-flex align-items-center text-white">
          <i class="bi bi-person-fill fs-4 me-2"></i>
          <span>{{ username }}</span>
          <button class="btn text-white ms-3" @click="logout">
            <i class="bi bi-box-arrow-right"></i> Logout
          </button>
        </div>
      </div>
    </nav>


    <!-- Centered Title -->
    <header class="title-section">
      <h1>Data Quality Agent</h1>
    </header>

    <!-- Main content -->
    <main class="main-content text-center">
      <data-quality-reports></data-quality-reports>
      <c-q-l-Check-table></c-q-l-Check-table>
    </main>

    <!-- Centered Footer -->
    <footer class="footer bg-secondary text-white text-center">
      © 2025 BBMRI-ERIC
    </footer>
  </div>
</template>

<script setup>
import CQLCheckTable from "../CQLCheckTable.vue";
import DataQualityReports from "../DataQualityReports.vue";
import {clearAuth, getUsername} from '../../js/api.js';
import { useRouter } from 'vue-router';
import {ref} from "vue";

const router = useRouter();
const username = ref(getUsername());

function logout() {
  clearAuth();
  router.replace('/login');
}
</script>

<style scoped>
/* Full height layout */
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* Shared background for title and main content */
.title-section,
.main-content {
  background-color: #e9ecef;
}

/* Centered title */
.title-section {
  text-align: center;
  padding: 2rem 1rem;
}

.title-section h1 {
  font-size: 3rem;
  font-weight: bold;
  margin: 0;
}

/* Main content grows */
.main-content {
  flex-grow: 1;
  padding: 2rem 1rem;
}

/* Footer styles */
.footer {
  padding: 1rem 0;
}
</style>
