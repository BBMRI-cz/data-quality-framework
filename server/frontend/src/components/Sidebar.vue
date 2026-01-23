<template>
  <aside class="sidebar" :class="{ show: showMobileMenu }">
    <div class="sidebar-container">
      <!-- Brand -->
      <router-link to="/" class="sidebar-brand">
        <div class="brand-icon">
          <img src="/logo.svg" alt="Logo" class="brand-logo" />
        </div>
        <div class="brand-text">
          <h4>Data Quality</h4>
          <small>Server</small>
        </div>
      </router-link>

      <!-- Navigation Menu -->
      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-link" :class="{ active: $route.name === 'Dashboard' }" @click="closeMobileMenu">
          <i class="bi bi-grid-3x3-gap-fill"></i>
          <span>Dashboard</span>
        </router-link>
        <router-link to="/agents" class="nav-link" :class="{ active: $route.name === 'Agents' }" @click="closeMobileMenu">
          <i class="bi bi-database-fill-gear"></i>
          <span>Agents</span>
        </router-link>
        <router-link to="/reports" class="nav-link" :class="{ active: $route.name === 'Reports' }" @click="closeMobileMenu">
          <i class="bi bi-bar-chart-fill"></i>
          <span>Reports</span>
        </router-link>
        <router-link to="/quality-checks" class="nav-link" :class="{ active: $route.name === 'QualityChecks' }" @click="closeMobileMenu">
          <i class="bi bi-clipboard-check-fill"></i>
          <span>Quality Checks</span>
        </router-link>
        <router-link to="/categories" class="nav-link" :class="{ active: $route.name === 'Categories' }" @click="closeMobileMenu">
          <i class="bi bi-tags-fill"></i>
          <span>Categories</span>
        </router-link>
        <router-link to="/groups" class="nav-link" :class="{ active: $route.name === 'Groups' }" @click="closeMobileMenu">
          <i class="bi bi-collection-fill"></i>
          <span>Groups</span>
        </router-link>
        <div class="nav-section">
          <div class="nav-section-header">
            <i class="bi bi-gear-fill"></i>
            <span>Settings</span>
          </div>
          <router-link to="/profile" class="nav-link nav-subitem" :class="{ active: $route.name === 'Profile' }" @click="closeMobileMenu">
            <i class="bi bi-person-fill"></i>
            <span>Profile</span>
          </router-link>
        </div>
      </nav>

      <!-- Copyright -->
      <div class="sidebar-footer">
        <Copyright />
      </div>
    </div>
  </aside>

  <!-- Mobile Menu Toggle -->
  <button class="mobile-menu-toggle" @click="toggleMobileMenu">
    <i class="bi bi-list"></i>
  </button>

  <!-- Mobile Overlay -->
  <div class="mobile-overlay" :class="{ show: showMobileMenu }" @click="closeMobileMenu"></div>
</template>

<script setup>
import { ref } from 'vue'
import Copyright from './Copyright.vue'

const showMobileMenu = ref(false)

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
}
</script>

<style scoped>
/* Sidebar */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  width: 260px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.sidebar-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 1.5rem 0;
}

/* Brand Section */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0 1.5rem;
  margin-bottom: 2rem;
  color: white;
  text-decoration: none;
  transition: all 0.2s ease;
}

.sidebar-brand:hover {
  transform: translateX(2px);
}

.brand-icon {
  width: 48px;
  height: 48px;
  min-width: 48px;
  background: white;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.2s ease;
  padding: 6px;
}

.brand-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.sidebar-brand:hover .brand-icon {
  background: rgba(255, 255, 255, 0.95);
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.brand-text h4 {
  margin: 0;
  font-weight: 600;
  font-size: 1.25rem;
  line-height: 1.2;
}

.brand-text small {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.875rem;
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0 1rem;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 1rem;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  border-radius: 12px;
  font-weight: 500;
  font-size: 0.95rem;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.nav-link i {
  font-size: 1.25rem;
  min-width: 1.25rem;
  transition: transform 0.2s ease;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  transform: translateX(2px);
}

.nav-link:hover i {
  transform: scale(1.1);
}

.nav-link.active {
  background: rgba(255, 255, 255, 0.25);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-link.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 60%;
  background: white;
  border-radius: 0 4px 4px 0;
}

/* Navigation Section */
.nav-section {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-section-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 1rem;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 600;
  font-size: 0.95rem;
  cursor: default;
}

.nav-section-header i {
  font-size: 1.25rem;
  min-width: 1.25rem;
}

.nav-subitem {
  margin-left: 1rem;
  padding-left: 2.25rem !important;
  font-size: 0.9rem;
}

.nav-subitem i {
  font-size: 1.125rem !important;
}

/* Sidebar Footer */
.sidebar-footer {
  padding: 1rem 1.5rem;
  margin-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.sidebar-footer :deep(.copyright) {
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.75rem;
  padding: 0;
}

.sidebar-footer :deep(.org-link) {
  color: white;
  font-weight: 500;
}

.sidebar-footer :deep(.org-link:hover) {
  color: rgba(255, 255, 255, 0.9);
}

.sidebar-footer :deep(.license-link) {
  color: white;
  font-weight: 500;
}

.sidebar-footer :deep(.license-link:hover) {
  color: rgba(255, 255, 255, 0.9);
}

.sidebar-footer :deep(.feedback-link) {
  color: white;
  font-weight: 500;
}

.sidebar-footer :deep(.feedback-link:hover) {
  color: rgba(255, 255, 255, 0.9);
}

.sidebar-footer :deep(.github-link) {
  color: white;
  font-weight: 500;
}

.sidebar-footer :deep(.github-link:hover) {
  color: rgba(255, 255, 255, 0.9);
}

/* Mobile Menu Toggle */
.mobile-menu-toggle {
  display: none;
  position: fixed;
  top: 1rem;
  left: 1rem;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  z-index: 1001;
  transition: all 0.2s ease;
}

.mobile-menu-toggle:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
}

.mobile-overlay {
  display: none;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .sidebar.show {
    transform: translateX(0);
  }

  .mobile-menu-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .mobile-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease;
    z-index: 999;
  }

  .mobile-overlay.show {
    opacity: 1;
    visibility: visible;
  }
}
</style>
