<template>
  <div class="copyright">
    <p class="mb-0">
      Developed by
      <a href="https://bbmri-eric.eu/" target="_blank" rel="noopener noreferrer" class="org-link">
        BBMRI-ERIC<sup>®</sup>
      </a>
      &
      <a href="https://mou.cz" target="_blank" rel="noopener noreferrer" class="org-link"> MMCI </a>
      under
      <a
        href="https://www.gnu.org/licenses/gpl-3.0.html"
        target="_blank"
        rel="noopener noreferrer"
        class="license-link"
      >
        GNU General Public License
      </a>
    </p>
    <p class="feedback-section">
      <a
        href="https://forms.cloud.microsoft/e/Vsc9Qbcka9"
        target="_blank"
        rel="noopener noreferrer"
        class="feedback-link"
      >
        <i class="bi bi-chat-square-text-fill" aria-hidden="true"></i>
        Give Feedback
      </a>
    </p>
    <p class="github-section">
      <a
        href="https://github.com/BBMRI-cz/data-quality-framework"
        target="_blank"
        rel="noopener noreferrer"
        class="github-link"
      >
        <i class="bi bi-github" aria-hidden="true"></i>
        View on GitHub
      </a>
    </p>
    <p class="docs-section">
      <a
        href="https://fdqf.bbmri-eric.eu/user/"
        target="_blank"
        rel="noopener noreferrer"
        class="feedback-link"
      >
        <i class="bi bi-book" aria-hidden="true"></i>
        Documentation
      </a>
    </p>
    <p class="swagger-section">
      <a :href="swaggerUrl" target="_blank" rel="noopener noreferrer" class="swagger-link">
        <i class="bi bi-braces-asterisk" aria-hidden="true"></i>
        API Documentation
      </a>
    </p>
    <p v-if="buildInfo" class="build-info">
      {{ buildInfo.version }} • {{ buildInfo.gitCommit
      }}<span v-if="buildInfo.buildTime"> • {{ formatBuildTime(buildInfo.buildTime) }}</span>
    </p>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { apiService } from '@/services/apiService';

  const buildInfo = ref(null);

  const swaggerUrl = computed(() => {
    const { protocol, hostname, port } = globalThis.location;
    const baseUrl = `${protocol}//${hostname}${port ? ':' + port : ''}`;
    return `${baseUrl}/api/swagger-ui/index.html`;
  });

  const formatBuildTime = (buildTime) => {
    if (!buildTime) return '';
    const date = new Date(buildTime);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  onMounted(async () => {
    try {
      buildInfo.value = await apiService.getInfo();
    } catch (error) {
      console.error('Failed to fetch build info:', error);
    }
  });
</script>

<style scoped>
  .copyright {
    text-align: center;
    padding: 1rem 0;
    color: var(--color-gray-500);
    font-size: 0.875rem;
  }

  .org-link {
    color: var(--color-primary);
    text-decoration: none;
    transition: color 0.2s ease;
    font-weight: 500;
  }

  .org-link:hover {
    color: var(--color-primary-dark);
    text-decoration: underline;
  }

  .license-link {
    color: var(--color-primary);
    text-decoration: none;
    transition: color 0.2s ease;
  }

  .license-link:hover {
    color: var(--color-primary-dark);
    text-decoration: underline;
  }

  .feedback-section {
    margin-top: 0.5rem;
    margin-bottom: 0;
  }

  .feedback-link {
    color: var(--color-primary);
    text-decoration: none;
    transition: color 0.2s ease;
    font-weight: 500;
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
  }

  .feedback-link i {
    font-size: 0.875rem;
  }

  .feedback-link:hover {
    color: var(--color-primary-dark);
    text-decoration: underline;
  }

  .github-section {
    margin-top: 0.25rem;
    margin-bottom: 0;
  }

  .github-link {
    color: var(--color-primary);
    text-decoration: none;
    transition: color 0.2s ease;
    font-weight: 500;
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
  }

  .github-link i {
    font-size: 0.875rem;
  }

  .github-link:hover {
    color: var(--color-primary-dark);
    text-decoration: underline;
  }

  .docs-section {
    margin-top: 0.25rem;
    margin-bottom: 0.25rem;
  }

  .swagger-section {
    margin-top: 0.25rem;
    margin-bottom: 0;
  }

  .swagger-link {
    color: var(--color-primary);
    text-decoration: none;
    transition: color 0.2s ease;
    font-weight: 500;
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
  }

  .swagger-link i {
    font-size: 0.875rem;
  }

  .swagger-link:hover {
    color: var(--color-primary-dark);
    text-decoration: underline;
  }

  sup {
    font-size: 0.7em;
  }

  .build-info {
    margin-top: 0.75rem;
    margin-bottom: 0;
    font-size: 0.7rem;
    color: var(--color-gray-500);
    opacity: 0.85;
  }
</style>
