package org.example.smashhub.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.auth.service.OtpService;
import org.example.smashhub.common.enums.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpServiceImpl implements OtpService {

    RedisTemplate<String, String> redisTemplate;

    @NonFinal
    @Value("${app.otp.expiration-minutes:5}")
    long otpExpirationMinutes;

    @NonFinal
    @Value("${app.otp.resend-cooldown-seconds:60}")
    long resendCooldownSeconds;

    private static final String OTP_PREFIX = "otp:verify:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generateOtp(OtpPurpose purpose, String email) {
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        redisTemplate.opsForValue()
                .set(otpKey(purpose, email), otp, Duration.ofMinutes(otpExpirationMinutes));
        log.info("Generated OTP for purpose={}, email={}, expires in {} minute(s)", purpose, email, otpExpirationMinutes);
        return otp;
    }

    @Override
    public boolean isValid(OtpPurpose purpose, String email, String otp) {
        String stored = redisTemplate.opsForValue().get(otpKey(purpose, email));
        return stored != null && stored.equals(otp);
    }

    @Override
    public void invalidate(OtpPurpose purpose, String email) {
        redisTemplate.delete(otpKey(purpose, email));
    }

    @Override
    public boolean isInCooldown(OtpPurpose purpose, String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(purpose, email)));
    }

    @Override
    public void markCooldown(OtpPurpose purpose, String email) {
        redisTemplate.opsForValue().set(cooldownKey(purpose, email), "1", Duration.ofSeconds(resendCooldownSeconds));
    }

    private String otpKey(OtpPurpose purpose, String email) {
        return OTP_PREFIX + purpose.name().toLowerCase() + ":" + email;
    }

    private String cooldownKey(OtpPurpose purpose, String email) {
        return COOLDOWN_PREFIX + purpose.name().toLowerCase() + ":" + email;
    }
}
