import { createRouter, createWebHistory } from 'vue-router'
import { authStore } from '../stores/authStore.js'

import Dashboard from '../views/Dashboard.vue'
import Reports from '../views/Reports.vue'
import Settings from '../views/Settings.vue'
import LoginView from '../views/LoginView.vue'
import NotFound from '../views/NotFound.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Home',
    beforeEnter: (to, from, next) => {
      if (authStore.isAuthenticated) {
        next('/dashboard')
      } else {
        next('/login')
      }
    }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: Reports,
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    if (to.path !== '/dashboard') {
      authStore.setRedirectPath(to.path)
    }
    next('/login')
  } else {
    next()
  }
})

export default router
