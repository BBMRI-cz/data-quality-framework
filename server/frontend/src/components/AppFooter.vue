<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-info">
        <p>&copy; {{ currentYear }} Data Quality Server. All rights reserved.</p>
        <p class="version">Version 1.0.0</p>
      </div>
      <div class="footer-links">
        <a href="#" class="footer-link">Privacy Policy</a>
        <a href="#" class="footer-link">Terms of Service</a>
        <a href="#" class="footer-link">Support</a>
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
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding: 1rem;
  z-index: 100;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.footer-info p {
  margin: 0;
  color: #6c757d;
  font-size: 0.875rem;
}

.version {
  font-size: 0.75rem !important;
  opacity: 0.8;
}

.footer-links {
  display: flex;
  gap: 1.5rem;
}

.footer-link {
  color: #667eea;
  text-decoration: none;
  font-size: 0.875rem;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
    text-align: center;
    gap: 0.5rem;
  }

  .footer-links {
    gap: 1rem;
  }
}
</style>
