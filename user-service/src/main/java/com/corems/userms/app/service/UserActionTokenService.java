package com.corems.userms.app.service;

import com.corems.common.security.service.TokenProvider;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.model.enums.UserActionType;
import com.corems.userms.app.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionTokenService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Value("${app.verification.email.expiration-hours:24}")
    private int emailVerificationExpirationHours;

    @Value("${app.verification.sms.expiration-minutes:120}")
    private int smsVerificationExpirationMinutes;

    @Value("${app.password-reset.expiration-hours:24}")
    private int passwordResetExpirationHours;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ACTION_TYPE_CLAIM = "action_type";
    private static final String USER_ID_CLAIM = "user_id";
    private static final String EMAIL_CLAIM = "email";

    @Transactional
    public void sendEmailVerification(UserEntity user) {
        String token = createActionToken(user, UserActionType.EMAIL_VERIFICATION, emailVerificationExpirationHours * 3600L);
        notificationService.sendEmailVerificationCode(user.getEmail(), user.getFirstName(), token);
        log.info("Email verification sent to user: {}", user.getEmail());
    }

    @Transactional
    public void sendSmsVerification(UserEntity user) {
        if (user.getPhoneNumber() == null) {
            throw new IllegalStateException("User does not have a phone number");
        }
        
        String code = generateNumericCode();
        notificationService.sendSmsVerificationCode(user.getPhoneNumber(), user.getFirstName(), code);
        log.info("SMS verification sent to user: {}", user.getPhoneNumber());
    }

    @Transactional
    public boolean verifyEmail(String email, String token) {
        try {
            if (!tokenProvider.isTokenValid(token)) {
                return false;
            }
            
            Claims claims = tokenProvider.getAllClaims(token);
            String actionType = claims.get(ACTION_TYPE_CLAIM, String.class);
            String tokenEmail = claims.get(EMAIL_CLAIM, String.class);
            
            if (!UserActionType.EMAIL_VERIFICATION.name().equals(actionType)) {
                return false;
            }
            
            if (!email.equals(tokenEmail)) {
                return false;
            }
            
            Optional<UserEntity> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                return false;
            }
            
            UserEntity user = userOptional.get();
            user.setEmailVerified(true);
            userRepository.save(user);
            
            log.info("Email verified for user: {}", email);
            return true;
        } catch (Exception e) {
            log.warn("Invalid email verification token for email: {}", email, e);
            return false;
        }
    }

    @Transactional
    public boolean verifyPhone(String phoneNumber, String code) {
        Optional<UserEntity> userOptional = userRepository.findByPhoneNumber(phoneNumber);
        if (userOptional.isEmpty()) {
            return false;
        }
        
        UserEntity user = userOptional.get();
        user.setPhoneVerified(true);
        userRepository.save(user);
        
        log.info("Phone verified for user: {}", phoneNumber);
        return true;
    }

    @Transactional
    public void resendVerification(String email, UserActionType actionType) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }
        
        UserEntity user = userOptional.get();
        
        switch (actionType) {
            case EMAIL_VERIFICATION -> {
                if (user.getEmailVerified()) {
                    throw new IllegalStateException("Email is already verified");
                }
                sendEmailVerification(user);
            }
            case SMS_VERIFICATION -> {
                if (user.getPhoneNumber() == null) {
                    throw new IllegalStateException("User does not have a phone number");
                }
                if (Boolean.TRUE.equals(user.getPhoneVerified())) {
                    throw new IllegalStateException("Phone is already verified");
                }
                sendSmsVerification(user);
            }
            default -> throw new IllegalArgumentException("Invalid verification type: " + actionType);
        }
    }

    @Transactional
    public void sendPasswordResetEmail(String email) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        UserEntity user = userOptional.get();
        String token = createActionToken(user, UserActionType.PASSWORD_RESET, passwordResetExpirationHours * 3600L);
        notificationService.sendPasswordResetEmail(user, token);
        
        log.info("Password reset email sent to user: {}", email);
    }

    @Transactional
    public boolean resetPassword(String email, String token, String newPassword) {
        try {
            if (!tokenProvider.isTokenValid(token)) {
                log.warn("Invalid password reset token for email: {}", email);
                return false;
            }
            
            Claims claims = tokenProvider.getAllClaims(token);
            String actionType = claims.get(ACTION_TYPE_CLAIM, String.class);
            String tokenEmail = claims.get(EMAIL_CLAIM, String.class);
            
            if (!UserActionType.PASSWORD_RESET.name().equals(actionType)) {
                log.warn("Invalid action type in password reset token for email: {}", email);
                return false;
            }
            
            if (!email.equals(tokenEmail)) {
                log.warn("Password reset token email mismatch. Token email: {}, provided email: {}", tokenEmail, email);
                return false;
            }
            
            Optional<UserEntity> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                return false;
            }
            
            UserEntity user = userOptional.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            log.info("Password successfully reset for user: {}", email);
            return true;
        } catch (Exception e) {
            log.warn("Invalid or expired password reset token for email: {}", email, e);
            return false;
        }
    }

    private String createActionToken(UserEntity user, UserActionType actionType, long expirationSeconds) {
        Map<String, Object> claims = Map.of(
                ACTION_TYPE_CLAIM, actionType.name(),
                USER_ID_CLAIM, user.getUuid().toString(),
                EMAIL_CLAIM, user.getEmail()
        );
        
        return tokenProvider.createAccessToken(UUID.randomUUID().toString(), claims);
    }

    private String generateNumericCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }
}