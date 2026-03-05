import '@fontsource/open-sans/400.css';
import '@fontsource/open-sans/500.css';
import '@fontsource/open-sans/600.css';
import '@fontsource/open-sans/700.css';
import '@fontsource/source-code-pro/400.css';
import '@fontsource/source-code-pro/500.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import './assets/main.css';

import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { initializeOidc } from './utils/oidc';

if (window.location.pathname === '/logged-in' || window.location.pathname === '/silent-renew') {
  initializeOidc().catch((error) => {
    console.error('OIDC initialization failed at app startup:', error);
  });
}

createApp(App).use(router).mount('#app');
