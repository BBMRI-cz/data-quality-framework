import { ref } from 'vue';
import { getUsername } from '../js/api.js';

export function useUserStore() {
  const isChangingPassword = ref(false);
  const passwordError = ref('');
  const passwordSuccess = ref('');

  async function changePassword(currentPassword, newPassword, confirmPassword) {
    passwordError.value = '';
    passwordSuccess.value = '';

    if (newPassword !== confirmPassword) {
      passwordError.value = 'New password and confirmation do not match';
      return false;
    }

    const passwordPattern = /^[a-zA-Z0-9!@#$%^&*(),.?":{}|<>_-]{8,}$/;
    if (!passwordPattern.test(newPassword)) {
      passwordError.value = 'Password must be at least 8 characters long and contain only letters, digits, or special characters';
      return false;
    }

    isChangingPassword.value = true;
    try {
      const username = getUsername();
      const response = await fetch('/api/user/password', {
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

      if (!response.ok) {
        if (response.status === 401) {
          passwordError.value = 'Invalid current password or authentication failed';
          return false;
        } else if (response.status === 400) {

          try {
            const errorResult = await response.json();
            passwordError.value = errorResult.message || errorResult.error || 'Validation failed';
          } catch (jsonError) {
            passwordError.value = 'Bad request - please check your input';
          }
          return false;
        } else {
          passwordError.value = `Server error: ${response.status}`;
          return false;
        }
      }

      const result = await response.json();
      if (result.success) {
        passwordSuccess.value = result.message || 'Password changed successfully!';
        return true;
      } else {
        passwordError.value = result.message || 'Failed to change password';
        return false;
      }
    } catch (error) {
      if (error instanceof SyntaxError && error.message.includes('JSON')) {
        passwordError.value = 'Server returned invalid response - please try again';
      } else {
        passwordError.value = 'An error occurred while changing password';
      }
      console.error('Password change error:', error);
      return false;
    } finally {
      isChangingPassword.value = false;
    }
  }

  return {
    isChangingPassword,
    passwordError,
    passwordSuccess,
    changePassword
  };
}
