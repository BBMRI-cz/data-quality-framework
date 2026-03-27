import { createOidc, useOidcStore } from 'vue3-oidc';
import settingsStore from '@/stores/settingsStore';
import { authStore } from '@/stores/authStore';

let oidcInitialized = false;
let oidcInitializationPromise = null;

async function fetchOidcSettings(forceFetch = false) {
  if (forceFetch || !settingsStore.settings.value.oidcAuthority) {
    await settingsStore.fetchOidcSettings();
  }

  const settings = settingsStore.settings.value;

  if (!settings.oidcAuthority) {
    throw new Error('OIDC authority is not configured');
  }
  if (!settings.oidcClientId) {
    throw new Error('OIDC client ID is not configured');
  }
  if (!settings.oidcRedirectUri) {
    throw new Error('OIDC redirect URI is not configured');
  }

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
    monitorSession: false,
  };
}

export async function initializeOidc(forceReinit = false) {
  if (oidcInitialized && !forceReinit) {
    return true;
  }

  if (oidcInitializationPromise && !forceReinit) {
    return oidcInitializationPromise;
  }

  if (forceReinit) {
    oidcInitialized = false;
    oidcInitializationPromise = null;
  }

  oidcInitializationPromise = (async () => {
    try {
      const oidcSettings = await fetchOidcSettings(forceReinit);

      const normalizedAuthority = oidcSettings.authority.replace(/\/+$/, '');
      const discoveryUrl = `${normalizedAuthority}/.well-known/openid-configuration`;
      const response = await fetch(discoveryUrl, {
        method: 'GET',
        mode: 'cors',
        cache: 'no-cache',
        signal: AbortSignal.timeout(5000),
      });

      if (!response.ok) {
        throw new Error(`OIDC server returned status ${response.status}`);
      }

      const oidcConfig = {
        oidcSettings,
        auth: false,
        events: {
          addUserLoaded: (user) => {
            if (user && authStore.mode === 'oidc') {
              authStore.setUser(user, user.access_token, 'oidc');
            }
          },
        },
      };

      createOidc(oidcConfig);
      oidcInitialized = true;
      return true;
    } catch (error) {
      console.error('OIDC initialization failed:', error);
      oidcInitializationPromise = null;
      oidcInitialized = false;
      throw new Error(
        'Unable to connect to authentication server. Please check if the server is running.'
      );
    }
  })();

  return oidcInitializationPromise;
}

export function resetOidc() {
  oidcInitialized = false;
  oidcInitializationPromise = null;
}

export async function signinWithOidc() {
  const oidcStore = useOidcStore();
  const userManager = oidcStore.state.value?.userManager;

  if (!userManager) {
    throw new Error('OIDC not initialized - userManager not available');
  }

  await userManager.signinRedirect();
}
