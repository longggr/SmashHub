package org.example.smashhub.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.request.AuthRequest;
import org.example.smashhub.dto.request.ChangePasswordRequest;
import org.example.smashhub.dto.request.ForgotPasswordRequest;
import org.example.smashhub.dto.request.IntrospectRequest;
import org.example.smashhub.dto.request.LogoutRequest;
import org.example.smashhub.dto.request.RefreshTokenRequest;
import org.example.smashhub.dto.request.ResetPasswordRequest;
import org.example.smashhub.dto.response.AuthResponse;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.entity.User;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.mapper.UserMapper;
import org.example.smashhub.repository.UserRepository;
import org.example.smashhub.shared.enums.OtpPurpose;
import org.example.smashhub.shared.enums.Status;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;

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
    LoginAttemptService loginAttemptService;
    TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        String email = request.getEmail();

        if (loginAttemptService.isLocked(email)) {
            long remainingSeconds = loginAttemptService.getLockoutSecondsRemaining(email);
            throw new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED,
                    String.format(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getMessage(), remainingSeconds));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == Status.LOCKED)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            boolean justLocked = loginAttemptService.registerFailure(email);
            if (justLocked) {
                long remainingSeconds = loginAttemptService.getLockoutSecondsRemaining(email);
                throw new AppException(ErrorCode.PASSWORD_ATTEMPT_EXCEEDED,
                        String.format(ErrorCode.PASSWORD_ATTEMPT_EXCEEDED.getMessage(), remainingSeconds));
            }
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        loginAttemptService.reset(email);

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }
    @Transactional
    public void logout(LogoutRequest request) throws ParseException, JOSEException{
        try{
            SignedJWT signedJWT = jwtService.verifyToken(request.getToken());
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            tokenBlacklistService.blacklist(jti, expiryTime);
        }catch (AppException e){
            log.info("Token already expired, logout skipped.");
        }
    }
    public AuthResponse refreshToken(RefreshTokenRequest request) throws ParseException , JOSEException{
        SignedJWT signedJWT = SignedJWT.parse(request.getToken());
        String jti = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        tokenBlacklistService.blacklist(jti,expiryTime);
        String username = signedJWT.getJWTClaimsSet().getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->new AppException(ErrorCode.USER_NOT_EXISTED));

        return AuthResponse.builder()
                .authenticated(true)
                .token(jwtService.generateToken(user))
                .build();

    }

    @Transactional
    public void sendVerificationOtp(User user) {
        String otp = otpService.generateOtp(OtpPurpose.REGISTER, user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), otp);
        otpService.markCooldown(OtpPurpose.REGISTER, user.getEmail());
    }
    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        if (user.getStatus() == Status.ACTIVE)
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);

        if (otpService.isInCooldown(OtpPurpose.REGISTER, email))
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);

        sendVerificationOtp(user);
    }

    @Transactional
    public UserResponse verifyAccount(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        if (user.getStatus() == Status.ACTIVE)
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);

        if (!otpService.isValid(OtpPurpose.REGISTER, email, otp))
            throw new AppException(ErrorCode.OTP_INVALID);
        user.setStatus(Status.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        otpService.invalidate(OtpPurpose.REGISTER, email);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        if (otpService.isInCooldown(OtpPurpose.RESET_PASSWORD, email))
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);

        String otp = otpService.generateOtp(OtpPurpose.RESET_PASSWORD, email);
        emailService.sendPasswordResetOtpEmail(email, otp);
        otpService.markCooldown(OtpPurpose.RESET_PASSWORD, email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword()))
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);

        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        if (!otpService.isValid(OtpPurpose.RESET_PASSWORD, email, request.getOtp()))
            throw new AppException(ErrorCode.OTP_INVALID);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        otpService.invalidate(OtpPurpose.RESET_PASSWORD, email);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword()))
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword()))
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_OLD);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

}
