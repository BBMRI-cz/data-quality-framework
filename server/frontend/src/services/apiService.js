import api from './api';

class ApiService {
    async login(username, password) {
        const response = await api.post('/auth/login', {
            username,
            password
        });
        const data = response.data;
        if (data.token) {
            localStorage.setItem('authToken', data.token);
        }
        return data;
    }

    async getAgents() {
        const response = await api.get('/v1/agents');
        return response.data;
    }

    async getAgent(agentId, expandInteractions = false) {
        const url = expandInteractions
            ? `/v1/agents/${agentId}?expand=interactions`
            : `/v1/agents/${agentId}`;
        const response = await api.get(url);
        return response.data;
    }

    async updateAgent(agentId, data) {
        const response = await api.patch(`/v1/agents/${agentId}`, data);
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
        const response = await api.delete(`/v1/agents/${agentId}`);
        return response.data;
    }

    async getAgentReports(agentId) {
        const response = await api.get(`/v1/agents/${agentId}/reports`);
        return response.data;
    }

    async getQualityChecks() {
        const response = await api.get('/v1/quality-checks');
        return response.data;
    }

    async getReports() {
        const response = await api.get('/v1/reports');
        return response.data;
    }

    async getReport(reportId) {
        const response = await api.get(`/v1/reports/${reportId}`);
        return response.data;
    }

    async updateQualityCheck(hash, data) {
        const response = await api.put(`/v1/quality-checks/${hash}`, data);
        return response.data;
    }

    async changePassword(userId, currentPassword, newPassword, confirmPassword) {
        await api.put(`/users/${userId}/password`, {
            currentPassword,
            newPassword,
            confirmPassword
        });
    }

    async getInfo() {
        const response = await api.get('/info');
        const data = response.data;
        return {
            version: data?.build?.version || 'unknown',
            gitCommit: data?.git?.commit?.id?.abbrev || 'unknown',
            buildTime: data?.build?.time || null
        };
    }

    async getCounts() {
        const response = await api.get('/counts');
        return response.data;
    }

    async getCategories() {
        const response = await api.get('/v1/categories');
        return response.data;
    }

    async getCategory(categoryId) {
        const response = await api.get(`/v1/categories/${categoryId}`);
        return response.data;
    }

    async createCategory(data) {
        const response = await api.post('/v1/categories', data);
        return response.data;
    }

    async updateCategory(categoryId, data) {
        const response = await api.put(`/v1/categories/${categoryId}`, data);
        return response.data;
    }

    async deleteCategory(categoryId) {
        const response = await api.delete(`/v1/categories/${categoryId}`);
        return response.data;
    }

    async getGroups() {
        const response = await api.get('/v1/groups');
        return response.data;
    }

    async getGroup(groupId) {
        const response = await api.get(`/v1/groups/${groupId}`);
        return response.data;
    }

    async createGroup(data) {
        const response = await api.post('/v1/groups', data);
        return response.data;
    }

    async updateGroup(groupId, data) {
        const response = await api.put(`/v1/groups/${groupId}`, data);
        return response.data;
    }

    async deleteGroup(groupId) {
        const response = await api.delete(`/v1/groups/${groupId}`);
        return response.data;
    }

    async assignAgentsToGroup(groupId, agentIds) {
        const response = await api.put(`/v1/groups/${groupId}/agents`, { agentIds });
        return response.data;
    }

    async getUserProfile() {
        const response = await api.get('/userinfo');
        return response.data;
    }

    async getUsers() {
        const response = await api.get('/v1/users');
        return response.data;
    }

    async getUser(userId) {
        const response = await api.get(`/v1/users/${userId}`);
        return response.data;
    }

    async createUser(data) {
        const response = await api.post('/v1/users', data);
        return response.data;
    }

    async updateUser(userId, data) {
        const response = await api.put(`/v1/users/${userId}`, data);
        return response.data;
    }

    async deleteUser(userId) {
        const response = await api.delete(`/v1/users/${userId}`);
        return response.data;
    }
}

export const apiService = new ApiService();
