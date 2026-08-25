package org.example.smashhub.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.request.IntrospectRequest;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.entity.User;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.mapper.UserMapper;
import org.example.smashhub.repository.UserRepository;
import org.example.smashhub.shared.enums.Status;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    PasswordEncoder passwordEncoder;
    JwtService jwtService;

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == Status.LOCKED)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            user.setFailedPassword(user.getFailedPassword() + 1);

            if (user.getFailedPassword() >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(Status.LOCKED);
                user.setFailedPassword(0);
                userRepository.save(user);
                throw new AppException(ErrorCode.PASSWORD_ATTEMPT_EXCEEDED);
            }

            userRepository.save(user);
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        user.setFailedPassword(0);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }





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
