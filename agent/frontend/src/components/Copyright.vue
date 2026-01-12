<template>
  <div class="copyright">
    <p class="mb-0">
      Developed by BBMRI-ERIC<sup>®</sup> & MMCI under
      <a
        href="https://www.gnu.org/licenses/gpl-3.0.html"
        target="_blank"
        rel="noopener noreferrer"
        class="license-link"
      >
        GNU General Public License
      </a>
    </p>
  </div>
</template>

<script setup>
// Copyright component for BBMRI-ERIC
</script>

<style scoped>
.copyright {
  text-align: center;
  padding: 1rem 0;
  color: #6b7280;
  font-size: 0.875rem;
}

.license-link {
  color: #667eea;
  text-decoration: none;
  transition: color 0.2s ease;
}

.license-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

sup {
  font-size: 0.7em;
}
</style>

