<template>
  <transition name="slide-up">
    <div v-if="showBanner" class="cookie-consent-banner">
      <div class="cookie-consent-content">
        <div class="cookie-consent-text">
          <h5 class="mb-2">
            <i class="bi bi-shield-check me-2"></i>
            Cookie Notice
          </h5>
          <p class="mb-0">
            This website uses essential browser storage (localStorage) to maintain your login session and improve your experience.
            We store your authentication token and user preferences locally. No tracking or analytics cookies are used.
          </p>
        </div>
        <div class="cookie-consent-actions">
          <button @click="acceptCookies" class="btn btn-primary btn-accept">
            <i class="bi bi-check-circle me-1"></i>
            Accept
          </button>
          <button
            @click="showDetails = !showDetails"
            class="btn btn-link btn-details"
            :aria-expanded="showDetails"
            aria-controls="cookie-details-section"
          >
            {{ showDetails ? 'Hide' : 'Details' }}
          </button>
        </div>
      </div>

      <transition name="expand">
        <div
          v-if="showDetails"
          class="cookie-details"
          id="cookie-details-section"
        >
          <div class="detail-section">
            <h6><i class="bi bi-key me-2"></i>Essential Storage</h6>
            <ul>
              <li><strong>Authentication Token:</strong> Required to keep you logged in</li>
              <li><strong>User Information:</strong> Stores your username and preferences</li>
            </ul>
            <p class="text-muted small mb-0">
              These are necessary for the website to function and cannot be disabled.
            </p>
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const showBanner = ref(false)
const showDetails = ref(false)

const CONSENT_KEY = 'cookieConsentGiven'

onMounted(() => {
  const consent = localStorage.getItem(CONSENT_KEY)
  if (!consent) {
    showBanner.value = true
  }
})

const acceptCookies = () => {
  localStorage.setItem(CONSENT_KEY, 'true')
  showBanner.value = false
}
</script>

<style scoped>
.cookie-consent-banner {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 3px solid #667eea;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  padding: 1.5rem;
}

.cookie-consent-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

.cookie-consent-text {
  flex: 1;
}

.cookie-consent-text h5 {
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
}

.cookie-consent-text p {
  color: #666;
  font-size: 0.95rem;
  line-height: 1.5;
}

.cookie-consent-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-shrink: 0;
}

.btn-accept {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  padding: 0.625rem 1.5rem;
  font-weight: 600;
  white-space: nowrap;
}

.btn-accept:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-details {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
  white-space: nowrap;
}

.btn-details:hover {
  color: #5a6fd8;
  text-decoration: underline;
}

.cookie-details {
  max-width: 1200px;
  margin: 1.5rem auto 0;
  padding-top: 1.5rem;
  border-top: 1px solid #e0e0e0;
}

.detail-section h6 {
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
}

.detail-section ul {
  margin-bottom: 0.75rem;
  padding-left: 1.5rem;
}

.detail-section li {
  color: #666;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

.expand-enter-active,
.expand-leave-active {
  transition: max-height 0.3s ease, opacity 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 300px;
  opacity: 1;
}

@media (max-width: 768px) {
  .cookie-consent-content {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .cookie-consent-actions {
    flex-direction: column;
    width: 100%;
  }

  .cookie-consent-actions .btn {
    width: 100%;
  }

  .cookie-consent-banner {
    padding: 1rem;
  }

  .cookie-consent-text h5 {
    font-size: 1rem;
  }

  .cookie-consent-text p {
    font-size: 0.875rem;
  }
}
</style>

