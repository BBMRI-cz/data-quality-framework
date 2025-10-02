<template>
  <nav class="navbar">
    <div class="navbar-container">
      <!-- Brand -->
      <div class="navbar-brand">
        <div class="brand-icon">
          <i class="bi bi-bar-chart-fill"></i>
        </div>
        <div class="brand-text">
          <h4>Data Quality</h4>
          <small>Dashboard</small>
        </div>
      </div>

      <!-- Desktop Navigation -->
      <div class="navbar-nav desktop-nav">
        <router-link to="/dashboard" class="nav-link" :class="{ active: $route.name === 'Dashboard' }">
          <i class="bi bi-grid-3x3-gap-fill"></i>
          Dashboard
        </router-link>
        <router-link to="/reports" class="nav-link" :class="{ active: $route.name === 'Reports' }">
          <i class="bi bi-bar-chart-fill"></i>
          Reports
        </router-link>
        <router-link to="/settings" class="nav-link" :class="{ active: $route.name === 'Settings' }">
          <i class="bi bi-gear-fill"></i>
          Settings
        </router-link>
      </div>

      <!-- User Menu -->
      <div class="user-menu desktop-nav">
        <div class="user-info">
          <span>{{ authStore.user?.username || 'User' }}</span>
        </div>
        <div class="dropdown">
          <button @click="toggleUserDropdown" class="logout-btn" ref="dropdownToggle">
            {{ getUserInitials() }}
          </button>
          <div class="dropdown-menu" :class="{ show: showUserDropdown }">
            <div class="dropdown-header">
              {{ authStore.user?.username || 'User' }}
            </div>
            <div class="dropdown-divider"></div>
            <button @click="handleLogout" class="dropdown-item">
              <i class="bi bi-box-arrow-right"></i>
              Sign Out
            </button>
          </div>
        </div>
      </div>

      <!-- Mobile Menu Button -->
      <button class="mobile-menu-btn" @click="toggleMobileMenu">
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>

    <!-- Mobile Menu -->
    <div class="mobile-menu" :class="{ show: showMobileMenu }">
      <router-link to="/dashboard" class="mobile-nav-link" @click="closeMobileMenu">
        <i class="bi bi-grid-3x3-gap-fill"></i>
        Dashboard
      </router-link>
      <router-link to="/reports" class="mobile-nav-link" @click="closeMobileMenu">
        <i class="bi bi-bar-chart-fill"></i>
        Reports
      </router-link>
      <router-link to="/settings" class="mobile-nav-link" @click="closeMobileMenu">
        <i class="bi bi-gear-fill"></i>
        Settings
      </router-link>
      <button @click="handleLogout" class="mobile-logout-btn">
        <i class="bi bi-box-arrow-right"></i>
        Sign Out
      </button>
    </div>
  </nav>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authStore } from '../stores/authStore.js'
import { notificationService } from '../services/notificationService.js'

const router = useRouter()
const showMobileMenu = ref(false)
const showUserDropdown = ref(false)

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
}

const toggleUserDropdown = () => {
  showUserDropdown.value = !showUserDropdown.value
}

const handleLogout = () => {
  authStore.logout()
  notificationService.info('Signed Out', 'You have been successfully signed out.')
  router.push('/login')
  closeMobileMenu()
}

const getUserInitials = () => {
  const username = authStore.user?.username || 'U'
  return username.substring(0, 2).toUpperCase()
}
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 1rem 0;
  position: sticky;
  top: 0;
  width: 100vw;
  left: 0;
  right: 0;
  margin: 0;
  z-index: 1000;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.navbar-container {
  width: 100%;
  margin: 0;
  padding: 0 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-brand {
  display: flex;
  align-items: center;
  color: white;
}

.brand-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1rem;
  font-size: 20px;
}

.brand-text h4 {
  margin: 0;
  font-weight: bold;
  font-size: 1.5rem;
}

.brand-text small {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.9rem;
}

.navbar-nav {
  display: flex;
  gap: 0.5rem;
}

.nav-link {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  padding: 0.75rem 1.5rem;
  border-radius: 25px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s ease;
}

.nav-link:hover,
.nav-link.active {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-info {
  color: white;
  font-weight: 500;
}

.logout-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  color: white;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* Mobile styles */
.mobile-menu-btn {
  display: none;
  flex-direction: column;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  gap: 3px;
}

.mobile-menu-btn span {
  width: 25px;
  height: 3px;
  background: white;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.mobile-menu {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 1rem;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.mobile-nav-link,
.mobile-logout-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  color: white;
  text-decoration: none;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  font-size: 1rem;
  border-radius: 8px;
  margin-bottom: 0.5rem;
  cursor: pointer;
  transition: background 0.2s ease;
}

.mobile-nav-link:hover,
.mobile-logout-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* Dropdown styles */
.dropdown {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 0.5rem 0;
  min-width: 200px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.2s ease;
  z-index: 1000;
}

.dropdown-menu.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-header {
  padding: 0.75rem 1rem;
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
}

.dropdown-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 0.5rem 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  color: #374151;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
  text-align: left;
}

.dropdown-item:hover {
  background: #f3f4f6;
  color: #111827;
}

.dropdown-item i {
  color: #6b7280;
}

/* Responsive breakpoints */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 1rem;
  }

  .navbar-nav.desktop-nav {
    display: none;
  }

  .user-menu.desktop-nav {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .mobile-menu.show {
    display: block;
  }

  .brand-text small {
    display: none;
  }

  .brand-text h4 {
    font-size: 1.2rem;
  }
}

/* Force horizontal layout on desktop */
@media (min-width: 769px) {
  .navbar-nav {
    display: flex !important;
    flex-direction: row !important;
    gap: 0.5rem;
  }

  .user-menu {
    display: flex !important;
    flex-direction: row !important;
    align-items: center;
    gap: 1rem;
  }

  .desktop-nav {
    display: flex !important;
  }
}

@media (max-width: 480px) {
  .navbar-container {
    padding: 0 0.5rem;
  }

  .brand-icon {
    width: 40px;
    height: 40px;
    margin-right: 0.5rem;
    font-size: 16px;
  }
}
</style>
