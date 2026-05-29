package eu.bbmri_eric.quality.server.user;

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
 *
 * <p>This implementation uses immutable {@link Attempt} values stored in a {@link
 * ConcurrentHashMap}, updated atomically via {@code compute}, to remain safe under concurrent
 * requests from the same IP.
 */
@Service
class LoginAttemptServiceImpl implements LoginAttemptService {

  private static final Logger logger = LoggerFactory.getLogger(LoginAttemptServiceImpl.class);

  private static final int MAX_ATTEMPTS = 5;
  private static final long BLOCK_MILLIS = 15 * 60 * 1000L;

  private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

  @Override
  public void recordFailure(String ip) {
    attempts.compute(
        ip,
        (k, v) -> {
          long now = System.currentTimeMillis();
          if (v == null || now - v.lastFailTime > BLOCK_MILLIS) {
            return new Attempt(1, now);
          }
          return new Attempt(v.count + 1, now);
        });
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
      attempts.remove(ip, a);
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
    final int count;
    final long lastFailTime;

    Attempt(int count, long lastFailTime) {
      this.count = count;
      this.lastFailTime = lastFailTime;
    }
  }
}
