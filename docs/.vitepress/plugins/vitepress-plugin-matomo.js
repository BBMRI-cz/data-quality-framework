function normalizeMatomoBaseUrl(baseUrl) {
    if (!baseUrl) {
        return ''
    }

    return baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
}

export function createMatomoPlugin(options = {}) {
    const baseUrl = normalizeMatomoBaseUrl(options.baseUrl ?? process.env.MATOMO_BASE_URL ?? process.env.MATOMO_URL)
    const siteId = (options.siteId ?? process.env.MATOMO_SITE_ID ?? '').trim()

    if (!baseUrl || !siteId) {
        return {
            head: [],
            enhanceApp: () => {}
        }
    }

    return {
        head: [
            ['script', {}, `
var _paq = window._paq = window._paq || [];
_paq.push(['trackPageView']);
_paq.push(['enableLinkTracking']);
(function() {
  var u = ${JSON.stringify(baseUrl)};
  _paq.push(['setTrackerUrl', u + 'matomo.php']);
  _paq.push(['setSiteId', ${JSON.stringify(siteId)}]);
  var d = document, g = d.createElement('script'), s = d.getElementsByTagName('script')[0];
  g.async = true;
  g.src = u + 'matomo.js';
  s.parentNode.insertBefore(g, s);
})();
            `]
        ],
        enhanceApp({router}) {
            router.onAfterRouteChanged = (to) => {
                if (typeof window === 'undefined' || !window._paq) {
                    return
                }

                window._paq.push(['setCustomUrl', to])
                window._paq.push(['setDocumentTitle', document.title])
                window._paq.push(['trackPageView'])
            }
        }
    }
}

export function setupMatomoRouteTracking(router) {
    router.onAfterRouteChanged = (to) => {
        if (typeof window === 'undefined' || !window._paq) {
            return
        }

        window._paq.push(['setCustomUrl', to])
        window._paq.push(['setDocumentTitle', document.title])
        window._paq.push(['trackPageView'])
    }
}

