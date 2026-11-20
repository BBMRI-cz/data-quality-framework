import { beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';

vi.mock('vue-router', () => ({
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
    create: vi.fn(),
  },
}));

import ServerCreatePage from '@/views/ServerCreatePage.vue';
import { notificationService } from '@/services/notificationService.js';
import { serverService } from '@/services/serverService.js';

const SERVER_DATA = {
  name: 'Production Central Server',
  url: 'https://central.example.com',
  publicKey: '-----BEGIN PUBLIC KEY-----abc-----END PUBLIC KEY-----',
};

const REJECTION = {
  response: { status: 409, data: { detail: 'Agent already registered' } },
};

function mountPage() {
  return mount(ServerCreatePage, {
    global: {
      plugins: [createPinia()],
      stubs: { PageHeader: true, ActionButton: true },
    },
  });
}

async function fillForm(wrapper) {
  await wrapper.find('#serverName').setValue(SERVER_DATA.name);
  // The URL input lives in FormField's custom slot, which receives no id slot
  // prop, so it cannot be addressed by id.
  await wrapper.find('input[type="url"]').setValue(SERVER_DATA.url);
  await wrapper.find('#serverPublicKey').setValue(SERVER_DATA.publicKey);
}

function findTryAgainButton(wrapper) {
  return wrapper.findAll('button').find((button) => button.text().includes('Try again'));
}

describe('ServerCreatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('keeps the filled form values when registration is rejected', async () => {
    serverService.create.mockRejectedValue(REJECTION);
    const wrapper = mountPage();

    await fillForm(wrapper);
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(serverService.create).toHaveBeenCalledTimes(1);
    expect(wrapper.find('#serverName').element.value).toBe(SERVER_DATA.name);
    expect(wrapper.find('input[type="url"]').element.value).toBe(SERVER_DATA.url);
    expect(wrapper.find('#serverPublicKey').element.value).toBe(SERVER_DATA.publicKey);
  });

  it('shows the ProblemDetail detail message after a failed registration', async () => {
    serverService.create.mockRejectedValue(REJECTION);
    const wrapper = mountPage();

    await fillForm(wrapper);
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    const alert = wrapper.find('.alert-danger');
    expect(alert.exists()).toBe(true);
    expect(alert.text()).toContain('Agent already registered');

    expect(notificationService.error).toHaveBeenCalledWith(
      'Registration Failed',
      expect.stringContaining('Agent already registered')
    );
    expect(notificationService.success).not.toHaveBeenCalled();
  });

  it('retries the registration with the retained values via Try again', async () => {
    serverService.create.mockRejectedValueOnce(REJECTION).mockResolvedValue({ id: 1 });
    const wrapper = mountPage();

    await fillForm(wrapper);
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    const tryAgain = findTryAgainButton(wrapper);
    expect(tryAgain).toBeTruthy();
    await tryAgain.trigger('click');
    await flushPromises();

    expect(serverService.create).toHaveBeenCalledTimes(2);
    expect(serverService.create).toHaveBeenLastCalledWith(SERVER_DATA);
    expect(wrapper.find('.alert-danger').exists()).toBe(false);
    expect(findTryAgainButton(wrapper)).toBeUndefined();
  });

  it('does not offer a retry before the first failed attempt', async () => {
    const wrapper = mountPage();
    expect(findTryAgainButton(wrapper)).toBeUndefined();
    expect(wrapper.find('.alert-danger').exists()).toBe(false);
  });
});
