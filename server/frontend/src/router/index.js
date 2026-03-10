import { createRouter, createWebHistory } from 'vue-router';
import { authStore } from '../stores/authStore.js';

import Dashboard from '../views/Dashboard.vue';
import Reports from '../views/Reports.vue';
import ReportDetailPage from '../views/ReportDetailPage.vue';
import Settings from '../views/Settings.vue';
import Profile from '../views/Profile.vue';
import AgentsView from '../views/AgentsView.vue';
import AgentReportView from '../views/AgentReportView.vue';
import AgentInteractionsView from '../views/AgentInteractionsView.vue';
import QualityChecksView from '../views/QualityChecksView.vue';
import QualityCheckDetailView from '../views/QualityCheckDetailView.vue';
import CategoriesView from '../views/CategoriesView.vue';
import CategoryDetailView from '../views/CategoryDetailView.vue';
import LoginView from '../views/LoginView.vue';
import LoggedIn from '../views/LoggedIn.vue';
import OidcSettings from '../views/OidcSettings.vue';
import NotFound from '../views/NotFound.vue';
import GroupsView from '../views/GroupsView.vue';
import GroupDetailView from '../views/GroupDetailView.vue';
import UsersView from '../views/UsersView.vue';
import UserDetailView from '../views/UserDetailView.vue';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false, title: 'Login' },
  },
  {
    path: '/',
    name: 'Home',
    beforeEnter: (_to, _from) => {
      if (authStore.isAuthenticated) {
        return '/dashboard';
      } else {
        return '/login';
      }
    },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true, title: 'Dashboard' },
  },
  {
    path: '/reports',
    name: 'Reports',
    component: Reports,
    meta: { requiresAuth: true, title: 'Reports' },
  },
  {
    path: '/reports/:id',
    name: 'ReportDetail',
    component: ReportDetailPage,
    meta: { requiresAuth: true, title: 'Report Detail' },
  },
  {
    path: '/quality-checks',
    name: 'QualityChecks',
    component: QualityChecksView,
    meta: { requiresAuth: true, title: 'Quality Checks' },
  },
  {
    path: '/quality-checks/:hash',
    name: 'QualityCheckDetail',
    component: QualityCheckDetailView,
    meta: { requiresAuth: true, title: 'Quality Check Detail' },
  },
  {
    path: '/categories',
    name: 'Categories',
    component: CategoriesView,
    meta: { requiresAuth: true, title: 'Categories' },
  },
  {
    path: '/categories/new',
    name: 'CategoryCreate',
    component: CategoryDetailView,
    meta: { requiresAuth: true, title: 'Create Category' },
  },
  {
    path: '/categories/:id',
    name: 'CategoryDetail',
    component: CategoryDetailView,
    meta: { requiresAuth: true, title: 'Category Detail' },
  },
  {
    path: '/groups',
    name: 'Groups',
    component: GroupsView,
    meta: { requiresAuth: true, title: 'Groups' },
  },
  {
    path: '/groups/new',
    name: 'GroupCreate',
    component: GroupDetailView,
    meta: { requiresAuth: true, title: 'Create Group' },
  },
  {
    path: '/groups/:id',
    name: 'GroupDetail',
    component: GroupDetailView,
    meta: { requiresAuth: true, title: 'Group Detail' },
  },
  {
    path: '/users',
    name: 'Users',
    component: UsersView,
    meta: { requiresAuth: true, title: 'Users' },
  },
  {
    path: '/users/new',
    name: 'NewUser',
    component: UserDetailView,
    meta: { requiresAuth: true, title: 'Create User' },
  },
  {
    path: '/users/:id',
    name: 'UserDetail',
    component: UserDetailView,
    meta: { requiresAuth: true, title: 'User Detail' },
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { requiresAuth: true, title: 'Settings' },
  },
  {
    path: '/oidc-settings',
    name: 'OidcSettings',
    component: OidcSettings,
    meta: { requiresAuth: true, title: 'OIDC Settings' },
  },
  {
    path: '/logged-in',
    name: 'LoggedIn',
    component: LoggedIn,
    meta: { requiresAuth: false, title: 'Logged In' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true, title: 'Profile' },
  },
  {
    path: '/agents',
    name: 'Agents',
    component: AgentsView,
    meta: { requiresAuth: true, title: 'Agents' },
  },
  {
    path: '/agents/:uuid/reports',
    name: 'AgentReport',
    component: AgentReportView,
    meta: { requiresAuth: true, title: 'Agent Reports' },
  },
  {
    path: '/agents/:uuid/interactions',
    name: 'AgentInteractions',
    component: AgentInteractionsView,
    meta: { requiresAuth: true, title: 'Agent Interactions' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { requiresAuth: false, title: 'Page Not Found' },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, _from) => {
  if (to.name === 'Login' && authStore.isAuthenticated) {
    return '/dashboard';
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    authStore.setRedirectPath(to.fullPath);
    return '/login';
  }
});

export default router;
