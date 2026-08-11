import { createRouter, createWebHistory } from 'vue-router';
import { isAuthenticated } from '@/api';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/LoginPage.vue') },
    { path: '/', redirect: '/dashboard' },
    {
      path: '/dashboard',
      component: () => import('@/views/DashboardPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/quality-checks',
      component: () => import('@/views/QualityChecksPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/quality-checks/new',
      component: () => import('@/views/QualityCheckEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/quality-checks/:id/edit',
      component: () => import('@/views/QualityCheckEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/reports',
      component: () => import('@/views/ReportsPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/reports/:id',
      component: () => import('@/views/ReportDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/servers',
      component: () => import('@/views/ServersPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/servers/:id',
      component: () => import('@/views/ServerDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/categories',
      component: () => import('@/views/CategoriesPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/categories/new',
      component: () => import('@/views/CategoryEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/categories/:id/edit',
      component: () => import('@/views/CategoryEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      component: () => import('@/views/SettingsPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/privacy',
      component: () => import('@/views/DifferentialPrivacyPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/diagnostics',
      component: () => import('@/views/DiagnosticsPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('@/views/NotFound.vue'),
      meta: { requiresAuth: true },
    },
  ],
});

router.beforeEach((to) => {
  const authed = isAuthenticated();
  if (to.meta.requiresAuth && !authed) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && authed) {
    return { path: to.query.redirect || '/' };
  }
  return true;
});

export default router;
