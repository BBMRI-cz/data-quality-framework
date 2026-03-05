<template>
  <header class="top-navbar">
    <div class="top-navbar-content">
      <div class="top-navbar-spacer"></div>

      <!-- User Section -->
      <div class="user-section">
        <div class="user-avatar">
          <span class="user-initials">{{ getUserInitials() }}</span>
        </div>
        <button class="signout-btn" @click="handleLogout">
          <i class="bi bi-box-arrow-right"></i>
          <span>Sign Out</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
  import { useRouter } from 'vue-router';
  import { useAuth } from 'vue3-oidc';
  import { authStore } from '../stores/authStore.js';
  import { notificationService } from '../services/notificationService.js';
  import { initializeOidc } from '../utils/oidc.js';

  const router = useRouter();

  const handleLogout = async () => {
    const isOidcMode = authStore.mode === 'oidc';
    authStore.logout();

    if (isOidcMode) {
      const oidcSuccess = await performOidcSignout();
      if (!oidcSuccess) {
        router.push('/login');
      }
    } else {
      notificationService.info('Signed Out', 'You have been successfully signed out.');
      router.push('/login');
    }
  };

  const performOidcSignout = async () => {
    try {
      const { signoutRedirect } = useAuth();
      await signoutRedirect();
      return true;
    } catch (error) {
      console.error('OIDC logout error:', error);
      return false;
    }
  };

  const getUserInitials = () => {
    const username = authStore.user?.username?.trim();
    if (!username) return 'U';

    const parts = username.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].substring(0, Math.min(2, parts[0].length)).toUpperCase();
  };
</script>

<style scoped>
  /* Top Navigation Bar */
  .top-navbar {
    position: fixed;
    top: 0;
    left: 260px;
    right: 0;
    height: 64px;
    background: white;
    border-bottom: 1px solid #e5e7eb;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    z-index: 900;
    transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .top-navbar-content {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 1.5rem;
  }

  .top-navbar-spacer {
    flex: 1;
  }

  /* User Section */
  .user-section {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .user-avatar {
    width: 40px;
    height: 40px;
    min-width: 40px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 0.9rem;
    color: white;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  }

  .user-initials {
    letter-spacing: 0.5px;
  }

  .signout-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.625rem 1.25rem;
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    color: #374151;
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  }

  .signout-btn:hover {
    background: #fee2e2;
    border-color: #fecaca;
    color: #dc2626;
    box-shadow: 0 2px 4px rgba(220, 38, 38, 0.1);
  }

  .signout-btn i {
    font-size: 1.125rem;
    transition: transform 0.2s ease;
  }

  .signout-btn:hover i {
    transform: translateX(2px);
  }

  /* Mobile Responsive */
  @media (max-width: 768px) {
    .top-navbar {
      left: 0;
      height: 56px;
    }

    .top-navbar-content {
      padding: 0 4rem 0 1rem;
    }

    .user-section {
      gap: 0.75rem;
    }

    .user-avatar {
      width: 36px;
      height: 36px;
      min-width: 36px;
      font-size: 0.85rem;
    }

    .signout-btn span {
      display: none;
    }

    .signout-btn {
      padding: 0.5rem;
      min-width: 40px;
      justify-content: center;
    }
  }
</style>
