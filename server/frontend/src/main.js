import './assets/main.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initializeOidc } from './utils/oidc'

if (window.location.pathname === '/logged-in' || window.location.pathname === '/silent-renew') {
  initializeOidc().catch(error => {
    console.error('OIDC initialization failed at app startup:', error)
  })
}

createApp(App).use(router).mount('#app')
