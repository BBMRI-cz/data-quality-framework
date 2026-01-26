import { createOidc } from 'vue3-oidc'
import settingsStore from '../stores/settingsStore'
import { authStore } from '../stores/authStore'

let oidcInitialized = false
let oidcInitializationPromise = null

async function fetchOidcSettings() {
    try {
        if (!settingsStore.settings.value.oidcAuthority) {
            await settingsStore.fetchOidcSettings()
        }

        const settings = settingsStore.settings.value

        return {
            authority: settings.oidcAuthority,
            client_id: settings.oidcClientId,
            redirect_uri: settings.oidcRedirectUri,
            post_logout_redirect_uri: settings.oidcPostLogoutRedirectUri || window.location.origin,
            response_type: 'code',
            scope: settings.oidcScopes,
            automaticSilentRenew: true,
            useRefreshToken: true,
            loadUserInfo: true,
            monitorSession: false
        }
    } catch (error) {
        console.error('Failed to fetch OIDC settings from backend:', error)
        const errorMessage = error instanceof Error && error.message
            ? error.message
            : JSON.stringify(error)
        throw new Error(`Could not load OIDC configuration: ${errorMessage}`)
    }
}


export async function initializeOidc() {
    if (oidcInitialized) {
        return true
    }

    if (oidcInitializationPromise) {
        return oidcInitializationPromise
    }

    oidcInitializationPromise = (async () => {
        try {
            const oidcSettings = await fetchOidcSettings()
            const oidcConfig = {
                oidcSettings,
                auth: false,
                events: {
                    addUserLoaded: (user) => {
                        if (user && authStore.mode === 'oidc') {
                            authStore.setUser(user, user.access_token, 'oidc')
                        }
                    }
                }
            }
            createOidc(oidcConfig)
            oidcInitialized = true
            return true
        } catch (error) {
            console.error('Failed to initialize OIDC:', error)
            oidcInitializationPromise = null
            throw error
        }
    })()

    return oidcInitializationPromise
}


