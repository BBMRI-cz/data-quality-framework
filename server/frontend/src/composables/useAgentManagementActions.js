import { ref } from 'vue';
import { apiService } from '@/services/apiService.js';
import { notificationService } from '@/services/notificationService.js';

export function useAgentManagementActions({ agent, agentId, error, onDeleted }) {
  const processing = ref(false);
  const showDeleteModal = ref(false);

  const approveAgent = async (agentToApprove) => {
    try {
      processing.value = true;
      await apiService.approveAgent(agentToApprove.id);
      agentToApprove.status = 'ACTIVE';
    } catch (err) {
      error.value = 'Failed to approve agent';
      console.error('Error approving agent:', err);
    } finally {
      processing.value = false;
    }
  };

  const declineAgent = async (agentToDecline) => {
    try {
      processing.value = true;
      await apiService.declineAgent(agentToDecline.id);
      agentToDecline.status = 'DECLINED';
    } catch (err) {
      error.value = 'Failed to decline agent';
      console.error('Error declining agent:', err);
    } finally {
      processing.value = false;
    }
  };

  const handleUpdateAgentName = async (newName) => {
    try {
      await apiService.updateAgentName(agentId.value, newName);

      if (agent.value) {
        agent.value.name = newName;
      }
    } catch (err) {
      error.value = 'Failed to update agent name';
      console.error('Error updating agent name:', err);
    }
  };

  const confirmDeleteAgent = () => {
    showDeleteModal.value = true;
  };

  const closeDeleteModal = () => {
    showDeleteModal.value = false;
  };

  const deleteAgent = async () => {
    try {
      processing.value = true;
      const agentNameToDelete = agent.value?.name || agent.value?.id || 'Agent';
      await apiService.deleteAgent(agentId.value);

      notificationService.success(
        'Agent Deleted',
        `${agentNameToDelete} has been successfully deleted.`
      );

      showDeleteModal.value = false;

      if (typeof onDeleted === 'function') {
        onDeleted();
      }
    } catch (err) {
      error.value = 'Failed to delete agent';
      console.error('Error deleting agent:', err);
      notificationService.error('Delete Failed', 'Could not delete the agent. Please try again.');
    } finally {
      processing.value = false;
    }
  };

  return {
    processing,
    showDeleteModal,
    approveAgent,
    declineAgent,
    handleUpdateAgentName,
    confirmDeleteAgent,
    closeDeleteModal,
    deleteAgent,
  };
}

