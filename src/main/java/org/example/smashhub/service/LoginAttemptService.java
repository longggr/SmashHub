package org.example.smashhub.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Theo doi so lan dang nhap sai lien tiep va khoa tam thoi tai khoan qua Redis
 * (khong dung cot DB) de:
 * - Tang counter atomic (INCR), tranh race condition khi bi brute-force dong thoi
 *   (khac voi doc-roi-ghi tren entity, von co the "mat" luot dem khi nhieu request
 *   toi cung luc).
 * - Tu dong het han khoa sau lockout-minutes, khong can admin mo tay.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginAttemptService {

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

    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKOUT_PREFIX + username));
    }

    /** So giay con lai truoc khi tu dong mo khoa. Dung de bao cho user biet cho bao lau. */
    public long getLockoutSecondsRemaining(String username) {
        Long ttl = redisTemplate.getExpire(LOCKOUT_PREFIX + username);
        return ttl == null ? 0 : Math.max(ttl, 0);
    }

    /**
     * Ghi nhan 1 lan sai mat khau (atomic qua Redis INCR).
     * Neu vua vuot nguong o lan nay, tu dong khoa va tra ve true.
     */
    public boolean registerFailure(String username) {
        String key = FAIL_PREFIX + username;
        Long attempts = redisTemplate.opsForValue().increment(key);

        // Chi set TTL o lan dau tao key, tranh moi lan sai lai reset lai window.
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

    public void lock(String username) {
        redisTemplate.opsForValue().set(LOCKOUT_PREFIX + username, "1", Duration.ofMinutes(lockoutMinutes));
    }

    /** Goi khi dang nhap thanh cong de xoa counter sai truoc do. */
    public void reset(String username) {
        redisTemplate.delete(FAIL_PREFIX + username);
    }
}