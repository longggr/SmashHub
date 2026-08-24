package org.example.smashhub.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.entity.User;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.mapper.UserMapper;
import org.example.smashhub.repository.UserRepository;
import org.example.smashhub.shared.enums.Status;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;
    UserMapper userMapper;
    OtpService otpService;
    EmailService emailService;

    /**
     * Sinh OTP moi va gui mail. Duoc goi ngay sau khi dang ky
     * va tu ben trong resendOtp(). Khong throw neu OTP sinh thanh cong,
     * nhung se propagate AppException(MAIL_SEND_FAILED) neu gui mail loi.
     */
    public void sendVerificationOtp(User user) {
        String otp = otpService.generateOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), otp);
        otpService.markCooldown(user.getEmail());
    }

    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        if (user.getStatus() == Status.ACTIVE)
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);

        if (otpService.isInCooldown(email))
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);

        sendVerificationOtp(user);
    }

    @Transactional
    public UserResponse verifyAccount(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        if (user.getStatus() == Status.ACTIVE)
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);

        if (!otpService.isValid(email, otp))
            throw new AppException(ErrorCode.OTP_INVALID);

        user.setStatus(Status.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        otpService.invalidate(email);

        return userMapper.toUserResponse(userRepository.save(user));
    }
}
