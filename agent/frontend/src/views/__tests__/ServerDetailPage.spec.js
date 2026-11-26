import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@/services/notificationService.js', () => ({
  notificationService: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

vi.mock('@/services/serverService.js', () => ({
  serverService: {
    get: vi.fn(),
    update: vi.fn(),
  },
}));

import ServerDetailPage from '@/views/ServerDetailPage.vue';
import { serverService } from '@/services/serverService.js';

const FAILURE_DESCRIPTION = [
  'Registration failed: This agent is already registered on the central server',
  'What to check: Ask the central server administrator to remove the existing registration of' +
    ' this agent, then remove and add the server here again.',
  'Technical detail: Agent agent-1 already exists [CONFLICT, HTTP 409]',
  'Server: https://central.example.com',
].join('\n');

function serverWith(interactions) {
  return {
    id: 1,
    name: 'Central Server',
    url: 'https://central.example.com',
    status: 'ERROR',
    clientId: 'client-1',
    publicKey: null,
    interactions,
  };
}

function mountPage() {
  return mount(ServerDetailPage, {
    // The details modal is teleported to document.body.
    attachTo: document.body,
    global: {
      stubs: { ActionButton: true },
    },
  });
}

function findDetailsButton(wrapper) {
  return wrapper.findAll('button').find((button) => button.text().trim() === 'Details');
}

describe('ServerDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('shows the full first line and opens a details modal for multi-line failures', async () => {
    serverService.get.mockResolvedValue(
      serverWith([
        {
          id: 1,
          type: 'REGISTRATION',
          timestamp: '2026-01-01T10:00:00Z',
          description: FAILURE_DESCRIPTION,
        },
      ])
    );
    const wrapper = mountPage();
    await flushPromises();

    const firstLine = wrapper.find('.description-first-line');
    expect(firstLine.exists()).toBe(true);
    expect(firstLine.classes()).toContain('text-danger');
    expect(firstLine.text()).toBe(
      'Registration failed: This agent is already registered on the central server'
    );
    expect(wrapper.text()).not.toContain('...');

    const detailsButton = findDetailsButton(wrapper);
    expect(detailsButton).toBeTruthy();
    await detailsButton.trigger('click');
    await flushPromises();

    expect(document.body.textContent).toContain('Interaction Details');
    expect(document.body.textContent).toContain('What to check:');
  });

  it('does not offer a details button for short single-line descriptions', async () => {
    serverService.get.mockResolvedValue(
      serverWith([
        { id: 1, type: 'COMMUNICATION', timestamp: '2026-01-01T10:00:00Z', description: 'OK' },
      ])
    );
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.find('.description-first-line').text()).toBe('OK');
    expect(findDetailsButton(wrapper)).toBeUndefined();
  });
});
