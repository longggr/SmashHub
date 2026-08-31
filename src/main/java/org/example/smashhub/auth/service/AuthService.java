package org.example.smashhub.auth.service;

import com.nimbusds.jose.JOSEException;
import org.example.smashhub.auth.dto.request.AuthRequest;
import org.example.smashhub.auth.dto.request.ForgotPasswordRequest;
import org.example.smashhub.auth.dto.request.LogoutRequest;
import org.example.smashhub.auth.dto.request.RefreshTokenRequest;
import org.example.smashhub.auth.dto.request.ResetPasswordRequest;
import org.example.smashhub.auth.dto.response.AuthResponse;
import org.example.smashhub.user.dto.request.ChangePasswordRequest;
import org.example.smashhub.user.dto.response.UserResponse;
import org.example.smashhub.user.entity.User;

import java.text.ParseException;

public interface AuthService {
    AuthResponse authenticate(AuthRequest request);
    void logout(LogoutRequest request) throws ParseException, JOSEException;
    AuthResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException;
    void sendVerificationOtp(User user);
    void resendOtp(String email);
    UserResponse verifyAccount(String email, String otp);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(String email, ChangePasswordRequest request);
}
