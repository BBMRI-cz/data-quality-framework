package eu.bbmri_eric.quality.agent.user;

/**
 * Tracks failed login attempts per client IP for brute-force protection. Provides methods to record
 * successes and failures, check if an IP is blocked, and periodically purge stale entries.
 */
public interface LoginAttemptService {

  /**
   * Records a failed login attempt for the given IP, incrementing the failure count and updating
   * the timestamp.
   *
   * @param ip the client IP address
   */
  void recordFailure(String ip);

  /**
   * Clears the failure history for the given IP, typically called after a successful login.
   *
   * @param ip the client IP address
   */
  void recordSuccess(String ip);

  /**
   * Checks whether the given IP is temporarily blocked due to too many failed login attempts.
   *
   * @param ip the client IP address
   * @return true if the IP is currently blocked, false otherwise
   */
  boolean isBlocked(String ip);
}
