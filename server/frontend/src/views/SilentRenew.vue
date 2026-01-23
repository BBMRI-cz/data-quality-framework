<template>
  <div>Renewing session...</div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useOidcStore } from 'vue3-oidc'
import { authStore } from '../stores/authStore'

onMounted(async () => {
  try {
    const oidcStore = useOidcStore()
    const maxAttempts = 100
    const intervalTime = 50
    let userManager = oidcStore.state.value?.userManager

    if (!userManager) {
      await new Promise((resolve) => {
        let attempts = 0
        const checkInterval = setInterval(() => {
          userManager = oidcStore.state.value?.userManager
          if (userManager || attempts++ >= maxAttempts) {
            clearInterval(checkInterval)
            resolve()
          }
        }, intervalTime)
      })
    }

    if (!userManager) {
      console.error('UserManager not available')
      authStore.setSilentRenewFailed(true)
      return
    }

    await userManager.signinSilentCallback(window.location.href)

  } catch (error) {
    console.error('Silent renew error:', error)
    authStore.setSilentRenewFailed(true)
  }
})
</script>
