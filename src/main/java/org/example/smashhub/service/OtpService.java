package org.example.smashhub.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService {
    RedisTemplate<String,String> redisTemplate;

    @NonFinal
    @Value("${app.otp.expiration-minutes:5}")
    long otpExpirationMinutes;

    @NonFinal
    @Value("${app.otp.resend-cooldown-seconds:60}")
    long resendCooldownSeconds;

    private static final String OTP_PREFIX = "otp:verify:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateOtp(String email){
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        redisTemplate.opsForValue()
                .set(OTP_PREFIX + email, otp, Duration.ofMinutes(otpExpirationMinutes));
        log.info("Generated OTP for purpose={}, email={}, expires in {} minute(s)", email, otpExpirationMinutes);
        return otp;
    }
    public boolean isValid(String email, String otp) {
        String stored = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        return stored != null && stored.equals(otp);
    }
    public void invalidate(String email) {
        redisTemplate.delete(OTP_PREFIX + email);
    }
    public boolean isInCooldown(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_PREFIX + email));
    }
    public void markCooldown(String email) {
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1", Duration.ofSeconds(resendCooldownSeconds));
    }
}
