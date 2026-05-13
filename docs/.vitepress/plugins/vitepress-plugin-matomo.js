function normalizeMatomoBaseUrl(baseUrl) {
    if (!baseUrl) {
        return ''
    }

    return baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
}

function resolveMatomoSettings(options = {}) {
    return {
        baseUrl: normalizeMatomoBaseUrl(options.baseUrl ?? process.env.MATOMO_BASE_URL ?? process.env.MATOMO_URL),
        siteId: (options.siteId ?? process.env.MATOMO_SITE_ID ?? '').trim()
    }
}

function getMatomoRuntimeConfig() {
    if (typeof window === 'undefined') {
        return null
    }

    const config = window.__MATOMO_CONFIG__

    if (!config?.baseUrl || !config?.siteId) {
        return null
    }

    return config
}

export function hasMatomoRuntimeConfig() {
    return getMatomoRuntimeConfig() !== null
}

export function createMatomoConfigEntries(options = {}) {
    const {baseUrl, siteId} = resolveMatomoSettings(options)

    if (!baseUrl || !siteId) {
        return []
    }

    return [
        ['script', {}, `window.__MATOMO_CONFIG__ = ${JSON.stringify({baseUrl, siteId})};`]
    ]
}


export function trackMatomoPageView(url) {
    if (typeof window === 'undefined' || !window._paq) {
        return
    }

    window._paq.push(['setCustomUrl', url])
    window._paq.push(['setDocumentTitle', document.title])
    window._paq.push(['trackPageView'])
}

export function setupMatomoRouteTracking(router) {
    router.onAfterRouteChanged = (to) => {
        trackMatomoPageView(to)
    }
}

export {normalizeMatomoBaseUrl}

