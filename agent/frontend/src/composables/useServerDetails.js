import { ref, computed, watch, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { serverService } from '@/services/serverService.js';
import { notificationService } from '@/services/notificationService.js';
import { TERMINAL_REGISTRATION_STATES } from '@/utils/registrationStatus.js';

/**
 * Composable for managing server details page state and logic
 * @param {string|number} serverId - The server ID to fetch
 * @returns {Object} Server details state and methods
 */
export function useServerDetails(serverId) {
  const router = useRouter();

  const server = ref(null);
  const loading = ref(true);
  const error = ref(null);

  // Delete modal state
  const showDeleteModal = ref(false);
  const isDeleting = ref(false);

  // Pagination state (0-based)
  const currentPage = ref(0);
  const pageSize = 10;

  // Filter state
  const filterType = ref('');
  const searchQuery = ref('');

  // JSON Modal state
  const showJsonModal = ref(false);
  const selectedJson = ref('');

  // Registration status state
  const registration = ref(null);
  const registrationTimedOut = ref(false);
  const isRetryingRegistration = ref(false);
  let registrationTimer = null;
  let registrationDeadline = null;

  // Registration runs are asynchronous on the backend: poll while REGISTERING, bounded so a
  // hung run stops the polling instead of leaking the interval forever.
  const REGISTRATION_POLL_INTERVAL_MS = 3000;
  const REGISTRATION_POLL_TIMEOUT_MS = 120000;

  const sortedInteractions = computed(() => {
    if (!server.value?.interactions) return [];
    return [...server.value.interactions].sort(
      (a, b) => new Date(b.timestamp) - new Date(a.timestamp)
    );
  });

  const filteredInteractions = computed(() => {
    let interactions = sortedInteractions.value;

    if (filterType.value) {
      interactions = interactions.filter((i) => i.type === filterType.value);
    }

    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase();
      interactions = interactions.filter((i) => i.description.toLowerCase().includes(query));
    }

    return interactions;
  });

  const totalPages = computed(() => {
    return Math.ceil(filteredInteractions.value.length / pageSize);
  });

  const paginatedInteractions = computed(() => {
    const start = currentPage.value * pageSize;
    const end = start + pageSize;
    return filteredInteractions.value.slice(start, end);
  });

  const formattedJson = computed(() => {
    if (!selectedJson.value) return '';
    try {
      const jsonObject = JSON.parse(selectedJson.value);
      return JSON.stringify(jsonObject, null, 2);
    } catch {
      return selectedJson.value;
    }
  });

  // Reset pagination when filters change
  watch([filterType, searchQuery], () => {
    currentPage.value = 0;
  });

  async function fetchServer() {
    loading.value = true;
    error.value = null;

    try {
      server.value = await serverService.get(serverId);
      registration.value = server.value?.registration || null;
      if (registration.value?.status === 'REGISTERING') {
        startRegistrationPolling();
      }
    } catch (err) {
      console.error('Error fetching server details:', err);
      error.value = err.response?.data?.message || 'Failed to load server details';
    } finally {
      loading.value = false;
    }
  }

  function stopRegistrationPolling() {
    if (registrationTimer) {
      clearInterval(registrationTimer);
      registrationTimer = null;
    }
  }

  function startRegistrationPolling() {
    if (registrationTimer) {
      return;
    }
    registrationTimedOut.value = false;
    registrationDeadline = Date.now() + REGISTRATION_POLL_TIMEOUT_MS;
    registrationTimer = setInterval(refreshRegistration, REGISTRATION_POLL_INTERVAL_MS);
  }

  async function refreshRegistration() {
    if (Date.now() >= registrationDeadline) {
      stopRegistrationPolling();
      registrationTimedOut.value = true;
      return;
    }
    try {
      registration.value = await serverService.getRegistration(serverId);
    } catch (err) {
      console.error('Error fetching registration state:', err);
      return;
    }
    if (TERMINAL_REGISTRATION_STATES.includes(registration.value?.status)) {
      stopRegistrationPolling();
      await fetchServer();
    }
  }

  async function retryRegistration() {
    isRetryingRegistration.value = true;
    try {
      await serverService.retryRegistration(serverId);
      registration.value = { ...registration.value, status: 'REGISTERING' };
      notificationService.info(
        'Registration Requested',
        'Registration request sent — waiting for the central server to confirm…'
      );
      startRegistrationPolling();
    } catch (err) {
      console.error('Error retrying registration:', err);
      notificationService.error(
        'Registration Retry Failed',
        err.response?.data?.detail ||
          err.response?.data?.message ||
          'Unable to request a registration retry. Please try again.'
      );
    } finally {
      isRetryingRegistration.value = false;
    }
  }

  onUnmounted(stopRegistrationPolling);

  async function deleteServer() {
    if (!server.value) return;

    isDeleting.value = true;
    try {
      await serverService.delete(server.value.id);
      notificationService.success(
        'Server Deleted',
        `${server.value.name} has been deleted successfully`
      );
      router.push('/servers');
    } catch (err) {
      console.error('Error deleting server:', err);
      notificationService.error(
        'Delete Failed',
        err.response?.data?.message || 'Unable to delete server. Please try again.'
      );
    } finally {
      isDeleting.value = false;
      showDeleteModal.value = false;
    }
  }

  function onPageChanged(page) {
    currentPage.value = page;
  }

  function clearFilters() {
    filterType.value = '';
    searchQuery.value = '';
    currentPage.value = 0;
  }

  function openJsonModal(jsonString) {
    selectedJson.value = jsonString;
    showJsonModal.value = true;
  }

  function closeJsonModal() {
    showJsonModal.value = false;
    selectedJson.value = '';
  }

  function openDeleteModal() {
    showDeleteModal.value = true;
  }

  function closeDeleteModal() {
    showDeleteModal.value = false;
  }

  return {
    // State
    server,
    loading,
    error,
    currentPage,
    filterType,
    searchQuery,
    showDeleteModal,
    isDeleting,
    showJsonModal,
    formattedJson,
    registration,
    registrationTimedOut,
    isRetryingRegistration,

    // Computed
    filteredInteractions,
    paginatedInteractions,
    totalPages,

    // Methods
    fetchServer,
    retryRegistration,
    deleteServer,
    onPageChanged,
    clearFilters,
    openJsonModal,
    closeJsonModal,
    openDeleteModal,
    closeDeleteModal,
  };
}
