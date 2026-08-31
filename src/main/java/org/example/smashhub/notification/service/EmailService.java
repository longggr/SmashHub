package org.example.smashhub.notification.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp);

    void sendPasswordResetOtpEmail(String toEmail, String otp);
}
