// .vitepress/theme/index.js
import { h } from 'vue'
import DefaultTheme from 'vitepress/theme'
import './style.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import CookieConsentBanner from './CookieConsentBanner.vue'
import {setupMatomoRouteTracking} from '../plugins/vitepress-plugin-matomo.js'

export default {
  extends: DefaultTheme,
  Layout: () => {
    return h(DefaultTheme.Layout, null, {
      'layout-bottom': () => h(CookieConsentBanner)
    })
  },
  enhanceApp({ router }) {
    setupMatomoRouteTracking(router)
  }
}
