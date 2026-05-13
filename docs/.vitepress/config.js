import {defineConfig} from 'vitepress'
import {getLatestVersionSync} from './version.js'

const version = getLatestVersionSync();

const navItems = [
    {text: 'Home', link: '/'},
    {text: 'Docs', link: '/user/'},
    {text: 'Developers', link: '/developer/'},
    {
        text: 'Publications',
        items: [
            {
                text: 'Privacy-preserving data quality assessment for federated health data networks',
                link: 'https://doi.org/10.1186/s12911-025-03328-6'
            }
        ]
    }
];

if (version) {
    navItems.push({
        text: version,
        items: [
            {
                text: 'Changelog',
                link: 'https://github.com/bbmri-cz/data-quality-framework/releases'
            },
            {
                text: 'Releases',
                link: `https://github.com/bbmri-cz/data-quality-framework/releases`
            }
        ]
    });
}

export default defineConfig({
    title: 'FDQF',
    description: 'Comprehensive framework for monitoring and ensuring data quality in biomedical research environments',

    sitemap: {
        hostname: 'https://bbmri-cz.github.io/data-quality-framework/'
    },

    themeConfig: {
        logo: '/logo.svg',

        nav: navItems,

        sidebar: {
            '/user/': [
                {
                    text: 'User Guide',
                    items: [
                        {text: 'Overview', link: '/user/'},
                        {text: 'Hands‑On Guide', link: '/user/hands‑on_guide'},
                        {text: 'Privacy and Security', link: '/user/privacy'},
                        {text: 'Deployment', link: '/user/deployment'},
                        {text: 'Configuration', link: '/user/configuration'},
                        {text: 'OIDC Configuration', link: '/user/oidc-configuration'},
                        {
                            text: 'Troubleshooting',
                            items: [
                                {
                                    text: 'Why report contains N/A values',
                                    link: '/user/troubleshooting/report'
                                }
                            ]
                        },
                    ]
                }
            ],
            '/developer/': [
                {
                    text: 'Developer Guide',
                    items: [
                        {text: 'Overview', link: '/developer/'},
                        {text: 'Frontend', link: '/developer/frontend'},
                        {text: 'Contributing', link: '/developer/CONTRIBUTING'},
                    ]
                }
            ]
        },

        socialLinks: [
            {icon: 'github', link: 'https://github.com/bbmri-cz/data-quality-framework'}
        ],

        editLink: {
            pattern: 'https://github.com/BBMRI-cz/data-quality-framework/edit/master/docs/:path',
            text: 'Edit this page on GitHub'
        },

        footer: {
            message: 'Licensed under the GNU GPL v3.0 | Built with <a href="https://vitepress.dev/" target="_blank" rel="noopener noreferrer">VitePress</a> | Please cite <a href="https://doi.org/10.1186/s12911-025-03328-6" target="_blank" rel="noopener noreferrer">Tomášik et al. (2026)</a>',
            copyright: 'Copyright © 2026 BBMRI-ERIC® & Masaryk Memorial Cancer Institute'
        },

        search: {
            provider: 'local'
        },

        lastUpdated: {
            text: 'Last updated',
            formatOptions: {
                dateStyle: 'medium',
                timeStyle: 'short'
            }
        }
    },
    ignoreDeadLinks: [
        // ignore exact url "/playground"
        '/playground',
        // ignore all localhost links
        /^https?:\/\/localhost/,
        // ignore all links include "/repl/""
        /\/repl\//,
        // custom function, ignore all links include "ignore"
        (url) => {
            return url.toLowerCase().includes('ignore')
        }
    ],
    markdown: {
        lineNumbers: true,
        linkify: true,
        config: (md) => {
            // Add any markdown-it plugins here if needed
        }
    },

    head: [
        ['link', {rel: 'icon', href: 'favicon.ico'}],
        ['link', {rel: 'alternate icon', href: 'favicon.ico'}],
        ['link', {rel: 'license', href: 'https://www.gnu.org/licenses/gpl-3.0.html'}],
        ['meta', {name: 'theme-color', content: '#667eea'}],
        ['meta', {name: 'viewport', content: 'width=device-width, initial-scale=1.0'}],
        ['meta', {name: 'dc.rights', content: 'https://www.gnu.org/licenses/gpl-3.0.html'}],
        ['meta', {name: 'dc.rights.license', content: 'GPL-3.0'}],
        ['meta', {property: 'og:type', content: 'website'}],
        ['meta', {property: 'og:locale', content: 'en'}],
        ['meta', {property: 'og:title', content: 'Data Quality Framework | Biomedical Data Quality Monitoring'}],
        ['meta', {property: 'og:site_name', content: 'Data Quality Framework'}],
        ['meta', {property: 'og:image', content: '/logo.svg'}],
        ['meta', {property: 'og:url', content: 'https://bbmri-cz.github.io/data-quality-framework/'}],
        ['meta', {
            property: 'og:description',
            content: 'Open-source, privacy-preserving framework for monitoring and ensuring data quality in biomedical research environments'
        }],
        ['meta', {name: 'twitter:card', content: 'summary_large_image'}],
        ['meta', {name: 'twitter:image', content: '/logo.svg'}]
    ]
})
