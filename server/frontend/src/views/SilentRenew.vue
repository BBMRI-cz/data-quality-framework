<template>
  <div>Token refresh is handled automatically...</div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  // This page is no longer needed for silent renewal
  // Redirect to dashboard or login
  console.warn('Silent renew page is deprecated - token refresh is now automatic')
  router.push('/login')
})
</script>
