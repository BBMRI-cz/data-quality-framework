import { ref } from 'vue';
import {getUsername, getDefaultPasswordFlag, getUserId} from '../js/api.js';

export function useUserStore() {
  const isChangingPassword = ref(false);
  const passwordError = ref('');
  const passwordSuccess = ref('');
  const defaultPasswordFlag = ref(getDefaultPasswordFlag());

  function initializeDefaultPasswordStatus() {
    defaultPasswordFlag.value = getDefaultPasswordFlag();
  }

  function updateDefaultPasswordStatus(status) {
    defaultPasswordFlag.value = status;
    if (typeof status === 'boolean') {
      sessionStorage.setItem('defaultPasswordFlag', status.toString());
    }
  }
  async function changePassword(currentPassword, newPassword, confirmPassword) {
    passwordError.value = '';
    passwordSuccess.value = '';

    if (newPassword !== confirmPassword) {
      passwordError.value = 'New password and confirmation do not match';
      return false;
    }

    isChangingPassword.value = true;
    try {
      const username = getUsername();
      const userId = getUserId();
      const response = await fetch(`/api/users/${userId}/password`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Basic ${btoa(username + ':' + currentPassword)}`
        },
        body: JSON.stringify({
          currentPassword,
          newPassword,
          confirmPassword
        })
      });

      if (response.ok) {
        passwordSuccess.value = 'Password changed successfully!';
        updateDefaultPasswordStatus(false);
        return true;
      } else {
        if (response.status === 401) {
          passwordError.value = 'Invalid current password';
        } else if (response.status === 400) {
          passwordError.value = 'Invalid password format or passwords do not match';
        } else if (response.status === 404) {
          passwordError.value = 'User not found';
        } else {
          passwordError.value = `Server error: ${response.status}`;
        }
        return false;
      }
    } catch (error) {
      console.error('Password change error:', error);
      passwordError.value = 'Network error - please try again';
      return false;
    } finally {
      isChangingPassword.value = false;
    }
  }

  return {
    isChangingPassword,
    passwordError,
    passwordSuccess,
    defaultPasswordFlag,
    changePassword,
    initializeDefaultPasswordStatus,
    updateDefaultPasswordStatus
  };
}
