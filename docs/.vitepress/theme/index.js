// .vitepress/theme/index.js
import { h } from 'vue'
import DefaultTheme from 'vitepress/theme'
import './style.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import {setupMatomoRouteTracking} from '../plugins/vitepress-plugin-matomo.js'

export default {
  extends: DefaultTheme,
  Layout: () => {
    return h(DefaultTheme.Layout, null, {
      // https://vitepress.dev/guide/extending-default-theme#layout-slots
    })
  },
  enhanceApp({ router }) {
    setupMatomoRouteTracking(router)
  }
}
