package org.example.smashhub.controller;

import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.dto.request.*;
import org.example.smashhub.dto.response.AuthResponse;
import org.example.smashhub.dto.response.IntrospectResponse;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.service.AuthService;
import org.example.smashhub.service.JwtService;
import org.example.smashhub.shared.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;
    JwtService jwtService;

    @PostMapping("/login")
    ApiResponse<AuthResponse> login (@RequestBody @Valid AuthRequest request){
        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .result(authService.authenticate(request))
                .message("login successfully")
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        return ApiResponse.<IntrospectResponse>builder()
                .result(jwtService.introspect(request))
                .build();

    }

    @PostMapping("/logout")
    ApiResponse<Void> logout (@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {
        authService.logout(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("logout successfully")
                .build();
    }

    @PostMapping("/refresh-token")
    ApiResponse<AuthResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest)
            throws ParseException, JOSEException {
        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .result(authService.refreshToken(refreshTokenRequest))
                .build();
    }

    @PostMapping("/verify-account")
    ApiResponse<UserResponse> verifyAccount(@RequestBody @Valid VerifyAccountRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(authService.verifyAccount(request.getEmail(), request.getOtp()))
                .message("Account verified successfully")
                .build();
    }

    @PostMapping("/resend-otp")
    ApiResponse<Void> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        authService.resendOtp(request.getEmail());
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("OTP has been resent to your email")
                .build();
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("OTP has been sent to your email")
                .build();
    }

    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Password reset successfully")
                .build();
    }
}
