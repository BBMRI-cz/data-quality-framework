import api from './api';

const AUTH_LOGIN_URL = '/auth/login';
const AGENTS_URL = '/v1/agents';
const QUALITY_CHECKS_URL = '/v1/quality-checks';
const REPORTS_URL = '/v1/reports';
const MANIFESTS_URL = '/v1/manifests';
const CATEGORIES_URL = '/v1/categories';
const GROUPS_URL = '/v1/groups';
const USERS_URL = '/v1/users';
const USERINFO_URL = '/userinfo';
const USER_PASSWORD_URL = '/users';
const PUBLIC_KEY_URL = '/v1/public-key';
const INFO_URL = '/info';
const COUNTS_URL = '/counts';

class ApiService {
  async login(username, password) {
    const response = await api.post(AUTH_LOGIN_URL, {
      username,
      password,
    });
    const data = response.data;
    if (data.token) {
      localStorage.setItem('authToken', data.token);
    }
    return data;
  }

  async getAgents() {
    const response = await api.get(AGENTS_URL);
    return response.data;
  }

  async getAgent(agentId, expandInteractions = false) {
    const url = expandInteractions
      ? `${AGENTS_URL}/${agentId}?expand=interactions`
      : `${AGENTS_URL}/${agentId}`;
    const response = await api.get(url);
    return response.data;
  }

  async updateAgent(agentId, data) {
    const response = await api.patch(`${AGENTS_URL}/${agentId}`, data);
    return response.data;
  }

  // Convenience methods using the consolidated updateAgent method
  async approveAgent(agentId) {
    return this.updateAgent(agentId, { status: 'ACTIVE' });
  }

  async declineAgent(agentId) {
    return this.updateAgent(agentId, { status: 'INACTIVE' });
  }

  async updateAgentStatus(agentId, status) {
    return this.updateAgent(agentId, { status });
  }

  async updateAgentName(agentId, name) {
    return this.updateAgent(agentId, { name });
  }

  async deleteAgent(agentId) {
    const response = await api.delete(`${AGENTS_URL}/${agentId}`);
    return response.data;
  }

  async getAgentReports(agentId, params = {}) {
    const response = await api.get(`${AGENTS_URL}/${agentId}/reports`, { params });
    return response.data;
  }

  async getQualityChecks() {
    const response = await api.get(QUALITY_CHECKS_URL);
    return response.data;
  }

  async getQualityCheck(id) {
    const response = await api.get(`${QUALITY_CHECKS_URL}/${id}`);
    return response.data;
  }

  // The list endpoint returns detailed checks (including versions), so no
  // per-check requests are needed to obtain version hashes.
  async getQualityChecksDetailed() {
    const data = await this.getQualityChecks();
    return data?._embedded?.qualityChecks || (Array.isArray(data) ? data : []);
  }

  async getReports(params = {}) {
    const response = await api.get(REPORTS_URL, { params });
    return response.data;
  }

  async getReport(reportId) {
    const response = await api.get(`${REPORTS_URL}/${reportId}`);
    return response.data;
  }

  async downloadReportSummary(reportId) {
    const response = await api.get(`${REPORTS_URL}/${reportId}/summary?format=pdf`, {
      responseType: 'blob',
    });
    const blob = new window.Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `report-${reportId}-summary.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }

  async createQualityCheck(data) {
    const response = await api.post(QUALITY_CHECKS_URL, data);
    return response.data;
  }

  async updateQualityCheck(id, data) {
    const response = await api.put(`${QUALITY_CHECKS_URL}/${id}`, data);
    return response.data;
  }

  async setKeywords(id, keywords) {
    const response = await api.put(`${QUALITY_CHECKS_URL}/${id}/keywords`, { keywords });
    return response.data;
  }

  async createQualityCheckVersion(id, query, type) {
    const body = type ? { query, type } : { query };
    const response = await api.post(`${QUALITY_CHECKS_URL}/${id}/versions`, body);
    return response.data;
  }

  async getManifests() {
    const response = await api.get(MANIFESTS_URL);
    return response.data;
  }

  async getManifest(id) {
    const response = await api.get(`${MANIFESTS_URL}/${id}`);
    return response.data;
  }

  async createManifest(data) {
    const response = await api.post(MANIFESTS_URL, data);
    return response.data;
  }

  async getManifestVersions(id) {
    const response = await api.get(`${MANIFESTS_URL}/${id}/versions`);
    return response.data;
  }

  async getManifestVersionQualityChecks(manifestId, versionId) {
    const response = await api.get(
      `${MANIFESTS_URL}/${manifestId}/versions/${versionId}/quality-checks`
    );
    return response.data;
  }

  async createManifestVersion(id, data) {
    const response = await api.post(`${MANIFESTS_URL}/${id}/versions`, data, {
      skipErrorNotification: true,
    });
    return response.data;
  }

  async getPublicKey() {
    const response = await api.get(PUBLIC_KEY_URL, { skipErrorNotification: true });
    return response.data;
  }

  async changePassword(userId, currentPassword, newPassword, confirmPassword) {
    await api.put(`${USER_PASSWORD_URL}/${userId}/password`, {
      currentPassword,
      newPassword,
      confirmPassword,
    });
  }

  async getInfo() {
    const response = await api.get(INFO_URL);
    const data = response.data;
    return {
      version: data?.build?.version || 'unknown',
      gitCommit: data?.git?.commit?.id?.abbrev || 'unknown',
      buildTime: data?.build?.time || null,
    };
  }

  async getCounts() {
    const response = await api.get(COUNTS_URL);
    return response.data;
  }

  async getCategories() {
    const response = await api.get(CATEGORIES_URL);
    return response.data;
  }

  async getCategory(categoryId) {
    const response = await api.get(`${CATEGORIES_URL}/${categoryId}`);
    return response.data;
  }

  async createCategory(data) {
    const response = await api.post(CATEGORIES_URL, data);
    return response.data;
  }

  async updateCategory(categoryId, data) {
    const response = await api.put(`${CATEGORIES_URL}/${categoryId}`, data);
    return response.data;
  }

  async deleteCategory(categoryId) {
    const response = await api.delete(`${CATEGORIES_URL}/${categoryId}`);
    return response.data;
  }

  async getGroups() {
    const response = await api.get(GROUPS_URL);
    return response.data;
  }

  async getGroup(groupId) {
    const response = await api.get(`${GROUPS_URL}/${groupId}`);
    return response.data;
  }

  async createGroup(data) {
    const response = await api.post(GROUPS_URL, data);
    return response.data;
  }

  async updateGroup(groupId, data) {
    const response = await api.put(`${GROUPS_URL}/${groupId}`, data);
    return response.data;
  }

  async deleteGroup(groupId) {
    const response = await api.delete(`${GROUPS_URL}/${groupId}`);
    return response.data;
  }

  async assignAgentsToGroup(groupId, agentIds) {
    const response = await api.put(`${GROUPS_URL}/${groupId}/agents`, { agentIds });
    return response.data;
  }

  async getUserProfile() {
    const response = await api.get(USERINFO_URL);
    return response.data;
  }

  async getUsers() {
    const response = await api.get(USERS_URL);
    return response.data;
  }

  async getUser(userId) {
    const response = await api.get(`${USERS_URL}/${userId}`);
    return response.data;
  }

  async createUser(data) {
    const response = await api.post(USERS_URL, data);
    return response.data;
  }

  async updateUser(userId, data) {
    const response = await api.put(`${USERS_URL}/${userId}`, data);
    return response.data;
  }

  async deleteUser(userId) {
    const response = await api.delete(`${USERS_URL}/${userId}`);
    return response.data;
  }
}

export const apiService = new ApiService();
