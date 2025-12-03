<template>
  <header class="top-navbar">
    <div class="top-navbar-content">
      <div class="top-navbar-spacer"></div>

      <!-- User Section -->
      <div class="user-section">
        <div v-if="defaultPasswordFlag" class="password-warning"
             @mouseenter="showTooltip = true"
             @mouseleave="showTooltip = false">
          <i class="bi bi-exclamation-triangle-fill text-warning"></i>
          <div v-if="showTooltip" class="warning-tooltip">
            For your account's security, please change your password.<br>
            Your current password is a system-generated default.
          </div>
        </div>

        <div class="user-dropdown">
          <button @click="toggleDropdown" class="user-dropdown-toggle">
            <div class="user-avatar">
              <span class="user-initials">{{ getUserInitials() }}</span>
            </div>
            <span class="username">{{ username }}</span>
            <i class="bi bi-chevron-down"></i>
          </button>

          <div v-if="showDropdown" class="dropdown-menu-custom">
            <button class="dropdown-item-custom" @click="showChangePasswordModal">
              <i class="bi bi-key me-2"></i>
              Change Password
              <span v-if="defaultPasswordFlag" class="badge bg-warning text-dark ms-2">Required</span>
            </button>
            <button class="dropdown-item-custom dropdown-item-signout" @click="handleLogout">
              <i class="bi bi-box-arrow-right me-2"></i>
              Sign Out
            </button>
          </div>
        </div>
      </div>
    </div>
  </header>

  <PasswordChangeModal :isVisible="showPasswordModal" @close="closePasswordModal" />
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth, getUsername } from '../js/api.js'
import { useUserStore } from '../stores/userStore.js'
import PasswordChangeModal from './PasswordChangeModal.vue'
import { notificationService } from '../services/notificationService.js'

const router = useRouter()
const username = ref(getUsername())
const showDropdown = ref(false)
const showPasswordModal = ref(false)
const showTooltip = ref(false)

const { defaultPasswordFlag, initializeDefaultPasswordStatus } = useUserStore()

const handleLogout = () => {
  clearAuth()
  notificationService.info('Signed Out', 'You have been successfully signed out.')
  router.push('/login')
}

const getUserInitials = () => {
  const user = username.value || 'U'
  return user.substring(0, 2).toUpperCase()
}

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const showChangePasswordModal = () => {
  showDropdown.value = false
  showPasswordModal.value = true
}

const closePasswordModal = () => {
  showPasswordModal.value = false
  initializeDefaultPasswordStatus()
}

const handleClickOutside = (event) => {
  if (showDropdown.value && !event.target.closest('.user-dropdown')) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  initializeDefaultPasswordStatus()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.top-navbar {
  position: fixed;
  top: 0;
  left: var(--sidebar-width);
  right: 0;
  height: var(--navbar-height);
  background: var(--bg-card);
  border-bottom: 1px solid var(--color-gray-200);
  box-shadow: var(--shadow-sm);
  z-index: var(--z-navbar);
  transition: left var(--transition-smooth);
}

.top-navbar-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-lg);
}

.top-navbar-spacer {
  flex: 1;
}

.user-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.password-warning {
  position: relative;
  cursor: pointer;
}

.password-warning i {
  font-size: 1.25rem;
}

.warning-tooltip {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: var(--spacing-sm);
  padding: 0.75rem;
  background: var(--color-gray-800);
  color: white;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  white-space: nowrap;
  box-shadow: var(--shadow-lg);
  z-index: var(--z-dropdown);
  min-width: 250px;
}

.warning-tooltip::before {
  content: '';
  position: absolute;
  top: -4px;
  right: var(--spacing-md);
  width: 8px;
  height: 8px;
  background: var(--color-gray-800);
  transform: rotate(45deg);
}

.user-dropdown {
  position: relative;
}

.user-dropdown-toggle {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--bg-card);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
}

.user-dropdown-toggle:hover {
  background: var(--bg-hover);
  border-color: var(--color-gray-300);
}

.user-avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  background: var(--gradient-primary);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
  color: white;
  box-shadow: var(--shadow-primary);
}

.username {
  font-weight: 500;
  color: var(--color-gray-700);
  font-size: 0.9rem;
}

.dropdown-menu-custom {
  position: absolute;
  top: calc(100% + var(--spacing-xs));
  right: 0;
  background: var(--bg-card);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  min-width: 200px;
  z-index: var(--z-dropdown);
  overflow: hidden;
}

.dropdown-item-custom {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0.75rem var(--spacing-md);
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  transition: background var(--transition-base);
  font-size: 0.9rem;
  color: var(--color-gray-700);
}

.dropdown-item-custom:hover {
  background: var(--color-gray-100);
}

.dropdown-item-signout {
  color: var(--color-danger);
}

.dropdown-item-signout:hover {
  background: #fee2e2;
}

@media (max-width: 768px) {
  .top-navbar {
    left: 0;
    height: var(--navbar-height-mobile);
  }

  .top-navbar-content {
    padding: 0 4rem 0 var(--spacing-md);
  }

  .user-section {
    gap: 0.75rem;
  }

  .user-avatar {
    width: 32px;
    height: 32px;
    min-width: 32px;
    font-size: 0.75rem;
  }

  .username {
    display: none;
  }

  .warning-tooltip {
    min-width: 200px;
    font-size: 0.75rem;
  }
}
</style>
