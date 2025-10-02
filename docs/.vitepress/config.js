import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Data Quality Framework',
  description: 'Comprehensive framework for monitoring and ensuring data quality in biomedical research environments',

  themeConfig: {
    logo: '/logo.svg',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'User Guide', link: '/user/' },
      { text: 'Developer Guide', link: '/developer/' }
    ],

    sidebar: {
      '/user/': [
        {
          text: 'User Guide',
          items: [
            { text: 'Getting Started', link: '/user/' }
          ]
        }
      ],
      '/developer/': [
        {
          text: 'Developer Guide',
          items: [
            { text: 'Overview', link: '/developer/' }
          ]
        }
      ]
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/bbmri-eric/data-quality-agent' }
    ],

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2025 BBMRI-ERIC'
    },

    search: {
      provider: 'local'
    }
  },

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    ['link', { rel: 'alternate icon', href: '/favicon.svg' }],
    ['meta', { name: 'theme-color', content: '#3b82f6' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:locale', content: 'en' }],
    ['meta', { property: 'og:title', content: 'Data Quality Framework | Biomedical Data Quality Monitoring' }],
    ['meta', { property: 'og:site_name', content: 'Data Quality Framework' }],
    ['meta', { property: 'og:url', content: 'https://your-domain.com/' }],
    ['meta', { property: 'og:description', content: 'Comprehensive framework for monitoring and ensuring data quality in biomedical research environments' }]
  ]
})
