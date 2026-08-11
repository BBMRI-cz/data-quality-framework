<template>
  <aside class="sidebar" :class="{ show: showMobileMenu }">
    <div class="sidebar-container">
      <!-- Brand -->
      <router-link to="/dashboard" class="sidebar-brand">
        <div class="brand-icon">
          <img src="/logo.svg" alt="Logo" class="brand-logo" />
        </div>
        <div class="brand-text">
          <h4>Data Quality</h4>
          <small>Agent</small>
        </div>
      </router-link>

      <!-- Navigation Menu -->
      <nav class="sidebar-nav">
        <router-link
          to="/dashboard"
          class="nav-link"
          :class="{ active: $route.path === '/dashboard' }"
          @click="closeMobileMenu"
        >
          <i class="bi bi-pie-chart-fill"></i>
          <span>Overview</span>
        </router-link>
        <router-link
          to="/quality-checks"
          class="nav-link"
          :class="{ active: $route.path === '/quality-checks' }"
          @click="closeMobileMenu"
        >
          <i class="bi bi-check-square-fill"></i>
          <span>Quality Checks</span>
        </router-link>
        <router-link
          to="/reports"
          class="nav-link"
          :class="{ active: $route.path === '/reports' }"
          @click="closeMobileMenu"
        >
          <i class="bi bi-file-earmark-text-fill"></i>
          <span>Reports</span>
        </router-link>
        <router-link
          to="/categories"
          class="nav-link"
          :class="{ active: $route.path.startsWith('/categories') }"
          @click="closeMobileMenu"
        >
          <i class="bi bi-tags-fill"></i>
          <span>Categories</span>
        </router-link>

        <!-- Settings Section (non-clickable parent) -->
        <div class="nav-section">
          <div class="nav-section-title">
            <i class="bi bi-gear-fill"></i>
            <span>Settings</span>
          </div>
          <div class="nav-section-items">
            <router-link
              to="/settings"
              class="nav-link nav-sublink"
              :class="{ active: $route.path === '/settings' }"
              @click="closeMobileMenu"
            >
              <i class="bi bi-database-fill-gear"></i>
              <span>Database</span>
            </router-link>
            <router-link
              to="/privacy"
              class="nav-link nav-sublink"
              :class="{ active: $route.path === '/privacy' }"
              @click="closeMobileMenu"
            >
              <i class="bi bi-person-fill-lock"></i>
              <span>Differential Privacy</span>
            </router-link>
            <router-link
              to="/servers"
              class="nav-link nav-sublink"
              :class="{ active: $route.path === '/servers' }"
              @click="closeMobileMenu"
            >
              <i class="bi bi-flag-fill"></i>
              <span>Reporting</span>
            </router-link>
            <router-link
              to="/diagnostics"
              class="nav-link nav-sublink"
              :class="{ active: $route.path === '/diagnostics' }"
              @click="closeMobileMenu"
            >
              <i class="bi bi-activity"></i>
              <span>Diagnostics</span>
            </router-link>
          </div>
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
  import { ref } from 'vue';
  import Copyright from './Copyright.vue';

  const showMobileMenu = ref(false);

  const toggleMobileMenu = () => {
    showMobileMenu.value = !showMobileMenu.value;
  };

  const closeMobileMenu = () => {
    showMobileMenu.value = false;
  };
</script>

<style scoped>
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    width: var(--sidebar-width);
    background: var(--gradient-primary);
    box-shadow: var(--shadow-md);
    z-index: var(--z-sidebar);
    display: flex;
    flex-direction: column;
  }

  .sidebar-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: var(--spacing-lg) 0;
  }

  .sidebar-brand {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    padding: 0 var(--spacing-lg);
    margin-bottom: var(--spacing-xl);
    color: white;
    text-decoration: none;
    transition: transform var(--transition-base);
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
    transition: all var(--transition-base);
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

  .sidebar-nav {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);
    padding: 0 var(--spacing-md);
  }

  .nav-link {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    padding: 0.875rem var(--spacing-md);
    color: rgba(255, 255, 255, 0.85);
    text-decoration: none;
    border-radius: 12px;
    font-weight: 500;
    font-size: 0.95rem;
    transition: all var(--transition-base);
    position: relative;
    overflow: hidden;
  }

  .nav-link i {
    font-size: 1.25rem;
    min-width: 1.25rem;
    transition: transform var(--transition-base);
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
    box-shadow: var(--shadow-sm);
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

  .nav-section {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
    margin-top: var(--spacing-sm);
  }

  .nav-section-title {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    padding: 0.875rem var(--spacing-md);
    color: rgba(255, 255, 255, 0.6);
    font-weight: 600;
    font-size: 0.85rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    cursor: default;
  }

  .nav-section-title i {
    font-size: 1.1rem;
    min-width: 1.1rem;
  }

  .nav-section-items {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .nav-sublink {
    padding-left: calc(var(--spacing-md) + 1.25rem + var(--spacing-md));
    font-size: 0.9rem;
  }

  .nav-sublink i {
    font-size: 1.1rem;
  }

  .sidebar-footer {
    padding: var(--spacing-md) var(--spacing-lg);
    margin-top: var(--spacing-md);
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

  .sidebar-footer :deep(.build-info) {
    color: rgba(255, 255, 255, 0.7);
  }

  .mobile-menu-toggle {
    display: none;
    position: fixed;
    top: var(--spacing-md);
    left: var(--spacing-md);
    width: 48px;
    height: 48px;
    background: var(--gradient-primary);
    border: none;
    border-radius: 12px;
    color: white;
    font-size: 1.5rem;
    cursor: pointer;
    box-shadow: var(--shadow-primary);
    z-index: calc(var(--z-sidebar) + 1);
    transition: all var(--transition-base);
  }

  .mobile-menu-toggle:hover {
    transform: scale(1.05);
    box-shadow: var(--shadow-primary-hover);
  }

  .mobile-overlay {
    display: none;
  }

  @media (max-width: 768px) {
    .sidebar {
      transform: translateX(-100%);
      transition: transform var(--transition-smooth);
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
      transition: all var(--transition-slow);
      z-index: var(--z-overlay);
    }

    .mobile-overlay.show {
      opacity: 1;
      visibility: visible;
    }
  }
</style>
