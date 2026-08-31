package org.example.smashhub.auth.service;

import org.example.smashhub.common.enums.OtpPurpose;

public interface OtpService {
    String generateOtp(OtpPurpose purpose, String email);
    boolean isValid(OtpPurpose purpose, String email, String otp);
    void invalidate(OtpPurpose purpose, String email);
    boolean isInCooldown(OtpPurpose purpose, String email);
    void markCooldown(OtpPurpose purpose, String email);
}
