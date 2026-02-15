import { defineStore } from 'pinia';
import { getDefaultPasswordFlag, setDefaultPasswordFlag, getUserId } from '@/api';
import { changePassword as changePasswordApi } from '@/services/authService.js';

export const useUserStore = defineStore('user', {
  state: () => ({
    isChangingPassword: false,
    passwordError: '',
    passwordSuccess: '',
    validationErrors: {},
    defaultPasswordFlag: getDefaultPasswordFlag(),
  }),

  actions: {
    initializeDefaultPasswordStatus() {
      this.defaultPasswordFlag = getDefaultPasswordFlag();
    },

    updateDefaultPasswordStatus(status) {
      this.defaultPasswordFlag = status;
      setDefaultPasswordFlag(status);
    },

    resetPasswordState() {
      this.passwordError = '';
      this.passwordSuccess = '';
      this.validationErrors = {};
    },

    async changePassword(currentPassword, newPassword, confirmPassword) {
      this.passwordError = '';
      this.passwordSuccess = '';
      this.validationErrors = {};

      if (newPassword !== confirmPassword) {
        this.passwordError = 'New password and confirmation do not match';
        return false;
      }

      this.isChangingPassword = true;
      try {
        const userId = getUserId();

        if (!userId) {
          this.passwordError = 'Not authenticated';
          return false;
        }

        await changePasswordApi(userId, currentPassword, newPassword, confirmPassword);
        this.passwordSuccess = 'Password changed successfully!';
        this.updateDefaultPasswordStatus(false);
        return true;
      } catch (error) {
        console.error('Password change error:', error);

        const status = error.response?.status;
        const errorData = error.response?.data;

        if (status === 401) {
          this.passwordError = 'Invalid current password';
        } else if (status === 400) {
          if (errorData?.validationErrors) {
            this.validationErrors = errorData.validationErrors;
          } else {
            this.passwordError =
              errorData?.detail || 'Invalid password format or passwords do not match';
          }
        } else if (status === 404) {
          this.passwordError = 'User not found';
        } else {
          this.passwordError = error.message || 'Network error - please try again';
        }
        return false;
      } finally {
        this.isChangingPassword = false;
      }
    },
  },
});
