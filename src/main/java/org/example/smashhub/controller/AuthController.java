package org.example.smashhub.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.dto.request.ResendOtpRequest;
import org.example.smashhub.dto.request.VerifyAccountRequest;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.service.AuthService;
import org.example.smashhub.shared.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

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
}
