package com.corems.userms.app.service;

import com.corems.common.security.service.TokenProvider;
import com.corems.userms.app.entity.ActionTokenEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.model.enums.AuthProvider;
import com.corems.userms.app.model.enums.UserActionType;
import com.corems.userms.app.repository.ActionTokenRepository;
import com.corems.userms.app.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionTokenService {

    private final UserRepository userRepository;
    private final ActionTokenRepository actionTokenRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Value("${app.verification-email.expiration-minutes:1440}")
    private int emailVerificationExpirationMinutes;

    @Value("${app.password-reset.expiration-minutes:1440}")
    private int passwordResetExpirationMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ACTION_TYPE_CLAIM = "action_type";
    private static final String USER_ID_CLAIM = "user_id";
    private static final String EMAIL_CLAIM = "email";
    private static final String TOKEN_ID_CLAIM = "token_id";

    @Transactional
    public void sendEmailVerification(UserEntity user) {
        actionTokenRepository.deleteByUserUuidAndActionType(user.getUuid(), UserActionType.EMAIL_VERIFICATION);
        
        String token = createActionToken(user, UserActionType.EMAIL_VERIFICATION, emailVerificationExpirationMinutes);
        notificationService.sendEmailVerificationCode(user.getEmail(), user.getFirstName(), token);
        log.info("Email verification sent to user: {}", user.getEmail());
    }

    @Transactional
    public void sendSmsVerification(UserEntity user) {
        if (user.getPhoneNumber() == null) {
            throw new IllegalStateException("User does not have a phone number");
        }
        
        // Delete any existing SMS verification tokens for this user
        actionTokenRepository.deleteByUserUuidAndActionType(user.getUuid(), UserActionType.SMS_VERIFICATION);
        
        String code = generateNumericCode();
        
        // Store the SMS code as an action token for validation
        UUID tokenId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10); // SMS codes expire in 10 minutes
        
        String codeHash = hashToken(code); // Hash the code for security
        
        ActionTokenEntity actionToken = ActionTokenEntity.builder()
                .uuid(tokenId)
                .tokenHash(codeHash)
                .actionType(UserActionType.SMS_VERIFICATION)
                .user(user)
                .expiresAt(expiresAt)
                .used(false)
                .build();
        
        actionTokenRepository.save(actionToken);
        
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
            
            String tokenHash = hashToken(token);
            Optional<ActionTokenEntity> actionTokenOpt = actionTokenRepository.findByTokenHashAndActionTypeAndUsedFalse(tokenHash, UserActionType.EMAIL_VERIFICATION);
            
            if (actionTokenOpt.isEmpty()) {
                log.warn("Action token not found or already used for email verification: {}", email);
                return false;
            }
            
            ActionTokenEntity actionToken = actionTokenOpt.get();
            if (actionToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Action token expired for email verification: {}", email);
                return false;
            }
            
            Optional<UserEntity> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                return false;
            }
            
            UserEntity user = userOptional.get();
            user.setEmailVerified(true);
            userRepository.save(user);
            
            actionToken.setUsed(true);
            actionToken.setUsedAt(LocalDateTime.now());
            actionTokenRepository.save(actionToken);
            
            log.info("Email verified for user: {}", email);
            return true;
        } catch (Exception e) {
            log.warn("Invalid email verification token for email: {}", email, e);
            return false;
        }
    }

    @Transactional
    public boolean verifyPhone(String phoneNumber, String code) {
        try {
            Optional<UserEntity> userOptional = userRepository.findByPhoneNumber(phoneNumber);
            if (userOptional.isEmpty()) {
                log.warn("No user found with phone number: {}", phoneNumber);
                return false;
            }
            
            UserEntity user = userOptional.get();
            String codeHash = hashToken(code);
            
            Optional<ActionTokenEntity> actionTokenOpt = actionTokenRepository
                .findByTokenHashAndActionTypeAndUsedFalseAndUserUuid(codeHash, UserActionType.SMS_VERIFICATION, user.getUuid());
            
            if (actionTokenOpt.isEmpty()) {
                log.warn("No valid SMS verification token found for phone number: {}", phoneNumber);
                return false;
            }
            
            ActionTokenEntity actionToken = actionTokenOpt.get();
            
            if (actionToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("SMS verification token expired for user: {}", user.getUuid());
                return false;
            }
            
            // Valid token found - mark phone as verified and token as used
            user.setPhoneVerified(true);
            userRepository.save(user);
            
            actionToken.setUsed(true);
            actionToken.setUsedAt(LocalDateTime.now());
            actionTokenRepository.save(actionToken);
            
            log.info("Phone verified for user: {} with phone: {}", user.getUuid(), phoneNumber);
            return true;
            
        } catch (Exception e) {
            log.warn("Invalid SMS verification code for phone: {}", phoneNumber, e);
            return false;
        }
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
        
        actionTokenRepository.deleteByUserUuidAndActionType(user.getUuid(), UserActionType.PASSWORD_RESET);
        
        String token = createActionToken(user, UserActionType.PASSWORD_RESET, passwordResetExpirationMinutes);
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
            
            String tokenHash = hashToken(token);
            Optional<ActionTokenEntity> actionTokenOpt = actionTokenRepository.findByTokenHashAndActionTypeAndUsedFalse(tokenHash, UserActionType.PASSWORD_RESET);
            
            if (actionTokenOpt.isEmpty()) {
                log.warn("Action token not found or already used for password reset: {}", email);
                return false;
            }
            
            ActionTokenEntity actionToken = actionTokenOpt.get();
            if (actionToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Action token expired for password reset: {}", email);
                return false;
            }
            
            Optional<UserEntity> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                return false;
            }
            
            UserEntity user = userOptional.get();
            if (!user.getProvider().contains(AuthProvider.local.name())) {
                user.setProvider(user.getProvider() + "," + AuthProvider.local.name());
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            actionToken.setUsed(true);
            actionToken.setUsedAt(LocalDateTime.now());
            actionTokenRepository.save(actionToken);
            
            log.info("Password successfully reset for user: {}", email);
            return true;
        } catch (Exception e) {
            log.warn("Invalid or expired password reset token for email: {}", email, e);
            return false;
        }
    }

    private String createActionToken(UserEntity user, UserActionType actionType, long expirationMinutes) {
        UUID tokenId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        
        Map<String, Object> claims = Map.of(
                ACTION_TYPE_CLAIM, actionType.name(),
                USER_ID_CLAIM, user.getUuid().toString(),
                EMAIL_CLAIM, user.getEmail(),
                TOKEN_ID_CLAIM, tokenId.toString()
        );
        
        String token = tokenProvider.createCustomToken(actionType.name(), tokenId.toString(), claims, expirationMinutes);
        String tokenHash = hashToken(token);
        
        ActionTokenEntity actionToken = ActionTokenEntity.builder()
                .uuid(tokenId)
                .tokenHash(tokenHash)
                .actionType(actionType)
                .user(user)
                .expiresAt(expiresAt)
                .used(false)
                .build();
        
        actionTokenRepository.save(actionToken);
        
        return token;
    }

    private String generateNumericCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public void cleanupExpiredTokens() {
        actionTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired action tokens");
    }
}