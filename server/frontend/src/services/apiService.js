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
            version: data?.build?.version || 'unknown'
        };
    }

    async getCounts() {
        const response = await api.get('/counts');
        return response.data;
    }
}

export const apiService = new ApiService();
