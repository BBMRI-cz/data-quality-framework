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
    info: vi.fn(),
  },
}));

vi.mock('@/services/serverService.js', () => ({
  serverService: {
    get: vi.fn(),
    update: vi.fn(),
    getRegistration: vi.fn(),
    retryRegistration: vi.fn(),
  },
}));

import ServerDetailPage from '@/views/ServerDetailPage.vue';
import { notificationService } from '@/services/notificationService.js';
import { serverService } from '@/services/serverService.js';

const FAILURE_DESCRIPTION = [
  'Registration failed: This agent is already registered on the central server',
  'What to check: Ask the central server administrator to remove the existing registration of' +
    ' this agent, then remove and add the server here again.',
  'Technical detail: Agent agent-1 already exists [CONFLICT, HTTP 409]',
  'Server: https://central.example.com',
].join('\n');

function serverWith(interactions, registration = null) {
  return {
    id: 1,
    name: 'Central Server',
    url: 'https://central.example.com',
    status: 'ERROR',
    clientId: 'client-1',
    publicKey: null,
    interactions,
    registration,
  };
}

const FAILED_REGISTRATION = {
  status: 'FAILED',
  lastErrorReason: 'UNREACHABLE',
  lastErrorMessage: 'Registration failed: Cannot reach the central server\nServer: https://x',
  registeredAt: null,
  attempts: 3,
};

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
    vi.useRealTimers();
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

  it('truncates long compact-JSON descriptions and keeps the JSON button', async () => {
    const description = JSON.stringify({
      total: 1000,
      passed: 850,
      failed: 150,
      details: 'x'.repeat(300),
    });
    serverService.get.mockResolvedValue(
      serverWith([{ id: 1, type: 'COMMUNICATION', timestamp: '2026-01-01T10:00:00Z', description }])
    );
    const wrapper = mountPage();
    await flushPromises();

    const preview = wrapper.find('.description-first-line').text();
    expect(preview.length).toBeLessThanOrEqual(121);
    expect(preview.endsWith('…')).toBe(true);

    const jsonButton = wrapper.findAll('button').find((button) => button.text().trim() === 'JSON');
    expect(jsonButton).toBeTruthy();
  });

  it('hides the registration card when the server carries no registration state', async () => {
    serverService.get.mockResolvedValue(serverWith([]));
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.find('.registration-card').exists()).toBe(false);
  });

  it('shows the registration badge, attempts and full error details for a failed registration', async () => {
    serverService.get.mockResolvedValue(serverWith([], FAILED_REGISTRATION));
    const wrapper = mountPage();
    await flushPromises();

    const card = wrapper.find('.registration-card');
    expect(card.exists()).toBe(true);
    const badge = card.find('.badge');
    expect(badge.classes()).toContain('bg-danger');
    expect(badge.text()).toBe('Failed');
    expect(card.text()).toContain('Automatic retry rounds: 3');
    expect(card.find('pre').text()).toBe(FAILED_REGISTRATION.lastErrorMessage);
    expect(card.text()).toContain('UNREACHABLE');
  });

  it('hides stale error details and shows the retry date while registering', async () => {
    serverService.get.mockResolvedValue(
      serverWith([], {
        status: 'REGISTERING',
        lastErrorReason: 'UNREACHABLE',
        lastErrorMessage: 'Registration failed: Cannot reach the central server',
        registeredAt: null,
        nextRetryAt: '2026-01-01T12:00:00Z',
        attempts: 1,
      })
    );
    const wrapper = mountPage();
    await flushPromises();

    const card = wrapper.find('.registration-card');
    expect(card.find('pre').exists()).toBe(false);
    expect(card.text()).not.toContain('UNREACHABLE');
    expect(card.text()).toContain('Next automatic retry:');
    wrapper.unmount();
  });

  it('offers a registration retry only for retryable states', async () => {
    serverService.get.mockResolvedValue(
      serverWith([], { ...FAILED_REGISTRATION, status: 'ACTIVE', attempts: 1 })
    );
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.find('.registration-card').text()).toContain('Active');
    expect(
      wrapper
        .findAllComponents({ name: 'ActionButton' })
        .some((c) => c.props('text') === 'Retry Registration')
    ).toBe(false);
  });

  it('polls the registration endpoint while registering and stops at a terminal state', async () => {
    vi.useFakeTimers();
    const approved = {
      status: 'REGISTERED_PENDING_APPROVAL',
      errorReason: null,
      errorMessage: null,
      registeredAt: '2026-01-01T10:00:00Z',
      attempts: 1,
    };
    serverService.get
      .mockResolvedValueOnce(
        serverWith([], {
          status: 'REGISTERING',
          errorReason: null,
          errorMessage: null,
          registeredAt: null,
          attempts: 0,
        })
      )
      .mockResolvedValue(serverWith([], approved));
    serverService.getRegistration
      .mockResolvedValueOnce({ status: 'REGISTERING', attempts: 1 })
      .mockResolvedValue(approved);

    const wrapper = mountPage();
    await vi.advanceTimersByTimeAsync(0);

    expect(serverService.getRegistration).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(3000);
    expect(serverService.getRegistration).toHaveBeenCalledTimes(1);
    expect(wrapper.find('.registration-card').text()).toContain('Registering');

    await vi.advanceTimersByTimeAsync(3000);
    await vi.advanceTimersByTimeAsync(0);
    expect(wrapper.find('.registration-card').text()).toContain('Registered - pending approval');

    const callsAtTerminal = serverService.getRegistration.mock.calls.length;
    await vi.advanceTimersByTimeAsync(9000);
    expect(serverService.getRegistration).toHaveBeenCalledTimes(callsAtTerminal);
  });

  it('shows the timeout notice and stops polling after two minutes of registering', async () => {
    vi.useFakeTimers();
    serverService.get.mockResolvedValue(
      serverWith([], {
        status: 'REGISTERING',
        errorReason: null,
        errorMessage: null,
        registeredAt: null,
        attempts: 0,
      })
    );
    serverService.getRegistration.mockResolvedValue({ status: 'REGISTERING', attempts: 2 });

    const wrapper = mountPage();
    await vi.advanceTimersByTimeAsync(0);

    await vi.advanceTimersByTimeAsync(117000);
    expect(wrapper.find('.registration-card').text()).not.toContain('Still registering');

    await vi.advanceTimersByTimeAsync(6000);
    expect(wrapper.find('.registration-card').text()).toContain(
      'Still registering — the agent retries automatically.'
    );
    const callsAtTimeout = serverService.getRegistration.mock.calls.length;
    await vi.advanceTimersByTimeAsync(9000);
    expect(serverService.getRegistration).toHaveBeenCalledTimes(callsAtTimeout);
  });

  it('retries registration from the card and switches the badge to registering', async () => {
    serverService.get.mockResolvedValue(serverWith([], FAILED_REGISTRATION));
    serverService.retryRegistration.mockResolvedValue(undefined);
    const wrapper = mountPage();
    await flushPromises();

    const retryButton = wrapper
      .findAllComponents({ name: 'ActionButton' })
      .find((c) => c.props('text') === 'Retry Registration');
    expect(retryButton).toBeTruthy();
    await retryButton.vm.$emit('click');
    await flushPromises();

    expect(serverService.retryRegistration).toHaveBeenCalledWith('1');
    expect(notificationService.info).toHaveBeenCalledWith(
      'Registration Requested',
      'Registration request sent — waiting for the central server to confirm…'
    );
    expect(wrapper.find('.registration-card .badge').text()).toBe('Registering');
    wrapper.unmount();
  });
});
