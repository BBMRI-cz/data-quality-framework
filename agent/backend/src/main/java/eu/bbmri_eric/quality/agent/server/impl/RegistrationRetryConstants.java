package eu.bbmri_eric.quality.agent.server.impl;

import java.time.Duration;

/**
 * Fixed tuning values shared by the registration retry collaborators ({@link
 * ServerRegistrationEventListener}, {@link RegistrationRetrySweep} and {@link ServerServiceImpl}).
 * They are deliberately not configurable: the retry budget is part of the documented contract and
 * must stay consistent across the dispatch (claim), the in-process attempts and the rescheduling.
 */
final class RegistrationRetryConstants {

  /** Registration attempts per registration run before the failure outcome is persisted. */
  static final int MAX_ATTEMPTS = 3;

  /** Initial backoff between registration attempts within a run, doubled with each attempt. */
  static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);

  /**
   * Delay from a dispatch or a persisted failed round to the next scheduled automatic retry; bounds
   * an unfinished registration run whose claim was left behind by a crash.
   */
  static final Duration RETRY_DELAY = Duration.ofMinutes(15);

  /** Automatic sweep rounds after which only a manual retry remains possible. */
  static final int MAX_SWEEP_ROUNDS = 8;

  private RegistrationRetryConstants() {}
}
