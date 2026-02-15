/**
 * Authentication state management
 * Handles token storage and retrieval
 */

const state = {
  authToken: null,
  defaultPasswordFlag: false,
};

export function clearAuth() {
  state.authToken = null;
  state.defaultPasswordFlag = false;
  sessionStorage.removeItem('authToken');
  sessionStorage.removeItem('username');
  sessionStorage.removeItem('userId');
}

export function isAuthenticated() {
  return !!state.authToken;
}

export function getUsername() {
  return sessionStorage.getItem('username');
}

export function getUserId() {
  return sessionStorage.getItem('userId');
}

export function getDefaultPasswordFlag() {
  return state.defaultPasswordFlag;
}

export function setDefaultPasswordFlag(flag) {
  if (typeof flag === 'boolean') {
    state.defaultPasswordFlag = flag;
  }
}

export function getAuthToken() {
  return state.authToken || sessionStorage.getItem('authToken');
}

export function setAuthToken(token) {
  state.authToken = token;
  sessionStorage.setItem('authToken', token);
}

export function setUserData(user, fallbackUsername) {
  const serverUsername =
    user?.username && String(user.username).trim() ? user.username : fallbackUsername;

  sessionStorage.setItem('username', serverUsername);

  if (user && typeof user.defaultPassword === 'boolean') {
    state.defaultPasswordFlag = user.defaultPassword;
  }

  if (user && user.userId) {
    sessionStorage.setItem('userId', user.userId.toString());
  }

  return {
    username: serverUsername,
    defaultPassword: user?.defaultPassword || false,
    userId: user?.userId || null,
  };
}

// Initialize token from sessionStorage on page load
function initializeAuth() {
  const storedToken = sessionStorage.getItem('authToken');
  if (storedToken) {
    state.authToken = storedToken;
  }
}

initializeAuth();
