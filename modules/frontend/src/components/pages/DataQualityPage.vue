<template>
  <div class="app-container">
    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm px-4">
      <div class="container-fluid">
        <div class="ms-auto d-flex align-items-center text-white">
          <i class="bi bi-person-fill fs-4 me-2"></i>
          <!-- Warning indicator for default password -->
          <div v-if="defaultPasswordFlag" class="position-relative me-2"
               @mouseenter="showTooltip = true"
               @mouseleave="showTooltip = false">
            <i class="bi bi-exclamation-triangle-fill text-warning fs-5"></i>
            <div v-if="showTooltip" class="custom-tooltip">
              You are using the default password. Please change it for security.
            </div>
          </div>

          <div class="position-relative">
            <span class="username-dropdown cursor-pointer" @click="toggleDropdown">
              {{ username }}
              <i class="bi bi-chevron-down ms-1"></i>
            </span>

            <div v-if="showDropdown" class="dropdown-menu-custom position-absolute">
              <button class="dropdown-item-custom" @click="showChangePasswordModal">
                <i class="bi bi-key me-2"></i>
                Change Password
                <span v-if="defaultPasswordFlag" class="badge bg-warning text-dark ms-2">Required</span>
              </button>
            </div>
          </div>
          <button class="btn text-white ms-3" @click="logout">
            <i class="bi bi-box-arrow-right"></i> Logout
          </button>
        </div>
      </div>
    </nav>

    <PasswordChangeModal :isVisible="showPasswordModal" @close="closePasswordModal" />

    <!-- Default Password Alert -->
    <div v-if="defaultPasswordFlag" class="alert alert-warning alert-dismissible fade show mx-4 mt-3" role="alert">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      <strong>Security Warning:</strong> You are using the default password. Please change it immediately for security reasons.
      <button @click="showChangePasswordModal" class="btn btn-warning btn-sm ms-2">
        Change Password
      </button>
    </div>

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
import PasswordChangeModal from "../PasswordChangeModal.vue";
import {clearAuth, getUsername} from '../../js/api.js';
import { useUserStore } from '../../stores/userStore.js';
import { useRouter } from 'vue-router';
import {ref, onMounted, onUnmounted, watch} from "vue";


const router = useRouter();
const username = ref(getUsername());
const showDropdown = ref(false);
const showPasswordModal = ref(false);
const showTooltip = ref(false);

const { defaultPasswordFlag, initializeDefaultPasswordStatus } = useUserStore();

watch(showPasswordModal, (newValue) => {
  if (!newValue) {
    initializeDefaultPasswordStatus();
  }
});

function toggleDropdown() {
  showDropdown.value = !showDropdown.value;
}

function showChangePasswordModal() {
  showDropdown.value = false;
  showPasswordModal.value = true;
}

async function closePasswordModal() {
  showPasswordModal.value = false;
}

function handleClickOutside(event) {
  if (showDropdown.value && !event.target.closest('.username-dropdown')) {
    showDropdown.value = false;
  }
}

function logout() {
  clearAuth();
  router.replace('/login');
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside);
  initializeDefaultPasswordStatus();
});
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
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

.cursor-pointer {
  cursor: pointer;
}
.username-dropdown {
  padding: 0.5rem;
  border-radius: 0.25rem;
  transition: background-color 0.2s;
}
.username-dropdown:hover {
  background-color: rgba(255, 255, 255, 0.1);
}
.dropdown-menu-custom {
  top: 100%;
  right: 0;
  background-color: white;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
  min-width: 200px;
  z-index: 1000;
  margin-top: 0.25rem;
}
.dropdown-item-custom {
  display: block;
  width: 100%;
  padding: 0.5rem 1rem;
  color: #212529;
  background: none;
  border: none;
  text-align: left;
  transition: background-color 0.2s;
}
.dropdown-item-custom:hover {
  background-color: #f8f9fa;
}
.custom-tooltip {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 0.5rem;
  border-radius: 0.25rem;
  white-space: nowrap;
  z-index: 1000;
}
</style>
