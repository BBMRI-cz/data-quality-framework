<template>
  <div>Renewing session...</div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useOidcStore } from 'vue3-oidc'

onMounted(async () => {
  try {
    const oidcStore = useOidcStore()
    let userManager = oidcStore.state.value?.userManager

    if (!userManager) {
      await new Promise((resolve) => {
        const maxAttempts = 100
        let attempts = 0
        const checkInterval = setInterval(() => {
          userManager = oidcStore.state.value?.userManager
          if (userManager || attempts++ >= maxAttempts) {
            clearInterval(checkInterval)
            resolve()
          }
        }, 50)
      })
    }

    if (!userManager) {
      console.error('UserManager not available')
      return
    }

    await userManager.signinSilentCallback(window.location.href)

  } catch (error) {
    console.error('Silent renew error:', error)
  }
})
</script>

<style scoped>
</style>

