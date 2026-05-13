<script setup>
import {computed, onMounted, ref} from 'vue'
import {hasMatomoRuntimeConfig} from '../plugins/vitepress-plugin-matomo.js'

const MATOMO_CONSENT_STORAGE_KEY = 'fdqf-matomo-consent'
const MATOMO_CONSENT_ACCEPTED = 'accepted'
const MATOMO_SCRIPT_ID = 'fdqf-matomo-tracker'

const consentState = ref('pending')
const matomoEnabled = ref(false)
const isVisible = computed(() => matomoEnabled.value && consentState.value === 'pending')

function getConsentState() {
  try {
    return window.localStorage.getItem(MATOMO_CONSENT_STORAGE_KEY) ?? 'pending'
  } catch {
    return 'pending'
  }
}

function setConsentState(value) {
  try {
    window.localStorage.setItem(MATOMO_CONSENT_STORAGE_KEY, value)
  } catch {
    // Ignore storage errors so the site still works without persistence.
  }
}

function ensureMatomoQueue() {
  window._paq = window._paq || []
  return window._paq
}

function ensureMatomoScriptLoaded(baseUrl) {
  if (document.getElementById(MATOMO_SCRIPT_ID)) {
    return
  }

  const script = document.createElement('script')
  script.id = MATOMO_SCRIPT_ID
  script.async = true
  script.src = `${baseUrl}matomo.js`

  const firstScript = document.getElementsByTagName('script')[0]
  if (firstScript?.parentNode) {
    firstScript.parentNode.insertBefore(script, firstScript)
  } else {
    document.head.appendChild(script)
  }
}

function loadMatomoTracker() {
  const config = window.__MATOMO_CONFIG__

  if (!config?.baseUrl || !config?.siteId) {
    return false
  }

  const queue = ensureMatomoQueue()
  queue.push(['setTrackerUrl', `${config.baseUrl}matomo.php`])
  queue.push(['setSiteId', config.siteId])
  queue.push(['enableLinkTracking'])
  ensureMatomoScriptLoaded(config.baseUrl)
  queue.push(['trackPageView'])
  return true
}

function acceptAnalytics() {
  setConsentState(MATOMO_CONSENT_ACCEPTED)
  consentState.value = 'accepted'
  loadMatomoTracker()
}

onMounted(() => {
  matomoEnabled.value = hasMatomoRuntimeConfig()

  if (!matomoEnabled.value) {
    return
  }

  consentState.value = getConsentState()

  if (consentState.value === 'accepted') {
    loadMatomoTracker()
  }
})
</script>

<template>
  <div v-if="isVisible" class="matomo-consent-banner" role="dialog" aria-describedby="matomo-consent-description">
    <div class="matomo-consent-banner__inner">
      <div class="matomo-consent-banner__content">
        <p id="matomo-consent-description" class="matomo-consent-banner__text">
          We use self-hosted Matomo analytics to understand how the docs are used.
        </p>
      </div>

      <div class="matomo-consent-banner__actions">
        <button class="VPButton medium alt matomo-consent-banner__button" type="button" @click="acceptAnalytics">
          I understand
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.matomo-consent-banner {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 60;
  padding: 0.45rem;
  pointer-events: none;
}

.matomo-consent-banner__inner {
  max-width: 740px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--vp-c-divider);
  border-radius: 11px;
  background: color-mix(in srgb, var(--vp-c-bg) 96%, var(--vp-c-brand-soft));
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
  pointer-events: auto;
}

.matomo-consent-banner__content {
  min-width: 0;
}

.matomo-consent-banner__text {
  margin: 0;
  color: var(--vp-c-text-2);
  line-height: 1.4;
  max-width: 52ch;
  font-size: 0.88rem;
}

.matomo-consent-banner__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  justify-content: flex-end;
  align-self: center;
  min-width: 0;
}

.matomo-consent-banner__actions :deep(.VPButton) {
  min-width: 124px;
  border-radius: 999px;
  font-size: 0.86rem;
  padding-inline: 0.9rem;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.matomo-consent-banner__button:hover,
.matomo-consent-banner__button:focus-visible {
  text-decoration: underline;
  text-underline-offset: 0.18em;
}

.matomo-consent-banner__button:hover {
  border-color: color-mix(in srgb, var(--vp-c-brand-1) 28%, var(--vp-c-divider));
  color: var(--vp-c-brand-1);
  background: color-mix(in srgb, var(--vp-c-brand-soft) 24%, transparent);
}

.matomo-consent-banner__button:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--vp-c-brand-1) 55%, white);
  outline-offset: 2px;
}

@media (max-width: 640px) {
  .matomo-consent-banner__inner {
    flex-direction: column;
    align-items: stretch;
    padding: 0.65rem;
  }

  .matomo-consent-banner__actions {
    justify-content: stretch;
    min-width: 0;
  }

  .matomo-consent-banner__actions :deep(.VPButton) {
    width: 100%;
    justify-content: center;
  }
}
</style>

