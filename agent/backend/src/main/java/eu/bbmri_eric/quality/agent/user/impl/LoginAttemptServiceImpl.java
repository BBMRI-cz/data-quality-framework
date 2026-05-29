package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.user.LoginAttemptService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * In-memory implementation of login attempt tracking. Enforces temporary lockout after a threshold
 * of consecutive failures and purges stale entries every hour to prevent memory leaks.
 */
@Service
class LoginAttemptServiceImpl implements LoginAttemptService {

  private static final Logger logger = LoggerFactory.getLogger(LoginAttemptServiceImpl.class);

  private static final int MAX_ATTEMPTS = 5;
  private static final long BLOCK_MILLIS = 15 * 60 * 1000L;

  private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

  @Override
  public void recordFailure(String ip) {
    Attempt a = attempts.computeIfAbsent(ip, k -> new Attempt());
    a.count++;
    a.lastFailTime = System.currentTimeMillis();
  }

  @Override
  public void recordSuccess(String ip) {
    attempts.remove(ip);
  }

  @Override
  public boolean isBlocked(String ip) {
    Attempt a = attempts.get(ip);
    if (a == null || a.count < MAX_ATTEMPTS) {
      return false;
    }
    if (System.currentTimeMillis() - a.lastFailTime > BLOCK_MILLIS) {
      attempts.remove(ip);
      return false;
    }
    logger.info("Login blocked for IP {} after {} failed attempts", ip, a.count);
    return true;
  }

  /** Purges stale entries older than the lockout window every hour. */
  @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
  void purgeOld() {
    long now = System.currentTimeMillis();
    attempts.entrySet().removeIf(e -> now - e.getValue().lastFailTime > BLOCK_MILLIS);
  }

  /** Clears all tracked attempts. Intended for test teardown only. */
  void clear() {
    attempts.clear();
  }

  private static class Attempt {
    int count;
    long lastFailTime;
  }
}
