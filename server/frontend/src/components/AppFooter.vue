<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-main">
        <div class="footer-brand">
          <h5 class="brand-title">Data Quality Server</h5>
          <p class="brand-subtitle">Ensuring data integrity and quality</p>
        </div>

        <div class="footer-info">
          <div class="developed-by">
            <span class="developed-text">Developed by</span>
            <span class="bbmri-brand">BBMRI-ERIC</span>
          </div>
          <div class="footer-details">
            <span class="copyright">&copy; {{ currentYear }} Licensed under GNU General Public License</span>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.app-footer {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem 0;
  margin-top: auto;
  position: relative;
  overflow: hidden;
}

.app-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  pointer-events: none;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 2rem;
  position: relative;
  z-index: 1;
}

.footer-main {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3rem;
  flex: 1;
  text-align: center;
}

.footer-brand {
  min-width: 200px;
}

.brand-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.brand-subtitle {
  margin: 0;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  font-style: italic;
}

.footer-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.developed-by {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
}

.developed-text {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 400;
}

.bbmri-brand {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 400;
  font-size: 1rem;
}

.footer-details {
  display: flex;
  gap: 1rem;
  align-items: center;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
}

/* Mobile responsive design */
@media (max-width: 768px) {
  .app-footer {
    padding: 1.5rem 0;
  }

  .footer-content {
    flex-direction: column;
    text-align: center;
    gap: 1.5rem;
    padding: 0 1rem;
  }

  .footer-main {
    flex-direction: column;
    gap: 1.5rem;
    width: 100%;
  }

  .footer-brand {
    min-width: unset;
  }

  .brand-title {
    font-size: 1.1rem;
  }

  .footer-info {
    align-items: center;
  }

  .footer-details {
    flex-direction: column;
    gap: 0.5rem;
  }
}

@media (max-width: 480px) {
  .footer-content {
    padding: 0 1rem;
  }
}
</style>
