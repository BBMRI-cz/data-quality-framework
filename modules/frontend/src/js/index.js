import { createRouter, createWebHistory } from 'vue-router';
import { isAuthenticated } from './api.js';

const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/login', component: () => import('../components/pages/LoginPage.vue') },
        { path: '/', component: () => import('../components/pages/DataQualityPage.vue'), meta: { requiresAuth: true } },
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