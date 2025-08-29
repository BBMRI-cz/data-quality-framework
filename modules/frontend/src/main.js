

import { createApp } from 'vue'
// main.js or main.ts
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.css'

// Optional: JavaScript for Bootstrap components (dropdowns, modals, etc.)
import 'bootstrap'
import App from './App.vue'
import router from './js/index.js';

createApp(App).use(router).mount('#app')
