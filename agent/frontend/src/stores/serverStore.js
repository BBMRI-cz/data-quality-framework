import { defineStore } from 'pinia';
import { serverService } from '@/services/serverService.js';

export const useServerStore = defineStore('server', {
  state: () => ({
    servers: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchServers() {
      this.loading = true;
      this.error = null;

      try {
        this.servers = await serverService.getAll();
        return this.servers;
      } catch (error) {
        this.error = 'Failed to fetch servers';
        console.error('Error fetching servers:', error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async createServer(serverData) {
      this.loading = true;
      this.error = null;

      try {
        const newServer = await serverService.create(serverData);
        this.servers.push(newServer);
        return newServer;
      } catch (error) {
        this.error = error.response?.data?.message || 'Failed to create server';
        console.error('Error creating server:', error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async updateServer(id, updateData) {
      this.loading = true;
      this.error = null;

      try {
        const updatedServer = await serverService.update(id, updateData);

        const index = this.servers.findIndex((server) => server.id === id);
        if (index !== -1) {
          this.servers[index] = updatedServer;
        }

        return updatedServer;
      } catch (error) {
        this.error = error.response?.data?.message || 'Failed to update server';
        console.error('Error updating server:', error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async deleteServer(id) {
      this.loading = true;
      this.error = null;

      try {
        await serverService.delete(id);

        const index = this.servers.findIndex((server) => server.id === id);
        if (index !== -1) {
          this.servers.splice(index, 1);
        }

        return true;
      } catch (error) {
        this.error = error.response?.data?.message || 'Failed to delete server';
        console.error('Error deleting server:', error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    clearError() {
      this.error = null;
    },
  },
});
