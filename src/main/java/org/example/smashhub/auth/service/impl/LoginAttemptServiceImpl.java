package org.example.smashhub.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.example.smashhub.auth.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginAttemptServiceImpl implements LoginAttemptService {

    RedisTemplate<String, String> redisTemplate;

    @NonFinal
    @Value("${app.login.max-failed-attempts:5}")
    int maxFailedAttempts;

    @NonFinal
    @Value("${app.login.failure-window-minutes:15}")
    long failureWindowMinutes;

    @NonFinal
    @Value("${app.login.lockout-minutes:15}")
    long lockoutMinutes;

    private static final String FAIL_PREFIX = "login:fail:";
    private static final String LOCKOUT_PREFIX = "login:lockout:";

    @Override
    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKOUT_PREFIX + username));
    }

    @Override
    public long getLockoutSecondsRemaining(String username) {
        Long ttl = redisTemplate.getExpire(LOCKOUT_PREFIX + username);
        return ttl == null ? 0 : Math.max(ttl, 0);
    }

    @Override
    public boolean registerFailure(String username) {
        String key = FAIL_PREFIX + username;
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(failureWindowMinutes));
        }

        if (attempts != null && attempts >= maxFailedAttempts) {
            lock(username);
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    @Override
    public void lock(String username) {
        redisTemplate.opsForValue().set(LOCKOUT_PREFIX + username, "1", Duration.ofMinutes(lockoutMinutes));
    }

    @Override
    public void reset(String username) {
        redisTemplate.delete(FAIL_PREFIX + username);
    }
}
