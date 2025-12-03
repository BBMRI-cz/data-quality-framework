import { createRouter, createWebHistory } from 'vue-router'
import { authStore } from '../stores/authStore.js'

import Dashboard from '../views/Dashboard.vue'
import Reports from '../views/Reports.vue'
import ReportDetailPage from '../views/ReportDetailPage.vue'
import Settings from '../views/Settings.vue'
import Profile from '../views/Profile.vue'
import AgentsView from '../views/AgentsView.vue'
import AgentReportView from '../views/AgentReportView.vue'
import AgentInteractionsView from '../views/AgentInteractionsView.vue'
import QualityChecksView from '../views/QualityChecksView.vue'
import QualityCheckDetailView from '../views/QualityCheckDetailView.vue'
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
    path: '/reports/:id',
    name: 'ReportDetail',
    component: ReportDetailPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/quality-checks',
    name: 'QualityChecks',
    component: QualityChecksView,
    meta: { requiresAuth: true }
  },
  {
    path: '/quality-checks/:hash',
    name: 'QualityCheckDetail',
    component: QualityCheckDetailView,
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  {
    path: '/agents',
    name: 'Agents',
    component: AgentsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/agents/:uuid/reports',
    name: 'AgentReport',
    component: AgentReportView,
    meta: { requiresAuth: true }
  },
  {
    path: '/agents/:uuid/interactions',
    name: 'AgentInteractions',
    component: AgentInteractionsView,
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
  // Redirect authenticated users away from login page
  if (to.name === 'Login' && authStore.isAuthenticated) {
    next('/dashboard')
    return
  }

  // Redirect unauthenticated users to login for protected routes
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
