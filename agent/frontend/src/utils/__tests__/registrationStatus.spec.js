import { describe, expect, it } from 'vitest';
import {
  TERMINAL_REGISTRATION_STATES,
  REGISTRATION_HELP_URL,
  formatRegistrationStatus,
  getRegistrationBadgeClass,
  getRegistrationTooltip,
  canRetryRegistration,
  getRegistrationHelpUrl,
} from '@/utils/registrationStatus.js';

describe('registrationStatus', () => {
  it('labels every known status', () => {
    expect(formatRegistrationStatus('UNREGISTERED')).toBe('Unregistered');
    expect(formatRegistrationStatus('REGISTERING')).toBe('Registering');
    expect(formatRegistrationStatus('REGISTERED_PENDING_APPROVAL')).toBe(
      'Registered - pending approval'
    );
    expect(formatRegistrationStatus('ACTIVE')).toBe('Active');
    expect(formatRegistrationStatus('FAILED')).toBe('Failed');
    expect(formatRegistrationStatus('SOMETHING_NEW')).toBe('SOMETHING_NEW');
  });

  it('maps statuses to bootstrap badge classes', () => {
    expect(getRegistrationBadgeClass('UNREGISTERED')).toBe('bg-secondary');
    expect(getRegistrationBadgeClass('REGISTERING')).toBe('bg-info text-dark');
    expect(getRegistrationBadgeClass('REGISTERED_PENDING_APPROVAL')).toBe('bg-warning text-dark');
    expect(getRegistrationBadgeClass('ACTIVE')).toBe('bg-success');
    expect(getRegistrationBadgeClass('FAILED')).toBe('bg-danger');
    expect(getRegistrationBadgeClass('SOMETHING_NEW')).toBe('bg-secondary');
  });

  it('provides a tooltip for every known status', () => {
    for (const status of Object.keys({
      UNREGISTERED: true,
      REGISTERING: true,
      REGISTERED_PENDING_APPROVAL: true,
      ACTIVE: true,
      FAILED: true,
    })) {
      expect(getRegistrationTooltip(status)).not.toBe('');
    }
    expect(getRegistrationTooltip('SOMETHING_NEW')).toBe('');
  });

  it('offers a retry only for FAILED and UNREGISTERED states', () => {
    expect(canRetryRegistration('FAILED')).toBe(true);
    expect(canRetryRegistration('UNREGISTERED')).toBe(true);
    expect(canRetryRegistration('REGISTERING')).toBe(false);
    expect(canRetryRegistration('REGISTERED_PENDING_APPROVAL')).toBe(false);
    expect(canRetryRegistration('ACTIVE')).toBe(false);
  });

  it('stops polling at terminal states only', () => {
    expect(TERMINAL_REGISTRATION_STATES).toEqual([
      'REGISTERED_PENDING_APPROVAL',
      'ACTIVE',
      'FAILED',
    ]);
  });

  it('links every registration failure reason to its documentation anchor', () => {
    const anchorByReason = {
      UNREACHABLE: 'unreachable',
      TIMEOUT: 'timeout',
      TLS: 'tls',
      UNAUTHORIZED: 'unauthorized',
      CONFLICT: 'conflict',
      RATE_LIMITED: 'rate-limited',
      CLIENT_ERROR: 'client-error',
      SERVER_ERROR: 'server-error',
      INCOMPLETE_RESPONSE: 'incomplete-response',
      UNKNOWN: 'unknown',
    };
    for (const [reason, anchor] of Object.entries(anchorByReason)) {
      expect(getRegistrationHelpUrl(reason)).toBe(`${REGISTRATION_HELP_URL}#${anchor}`);
    }
    expect(getRegistrationHelpUrl('PENDING_APPROVAL')).toBe(
      `${REGISTRATION_HELP_URL}#pending-approval`
    );
  });

  it('falls back to the bare help URL for missing reasons', () => {
    expect(getRegistrationHelpUrl(null)).toBe(REGISTRATION_HELP_URL);
    expect(getRegistrationHelpUrl('')).toBe(REGISTRATION_HELP_URL);
    expect(getRegistrationHelpUrl(undefined)).toBe(REGISTRATION_HELP_URL);
  });
});
