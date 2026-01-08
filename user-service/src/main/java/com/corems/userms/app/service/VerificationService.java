package com.corems.userms.app.service;

import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.entity.VerificationTokenEntity;
import com.corems.userms.app.entity.VerificationTokenEntity.VerificationType;
import com.corems.userms.app.repository.UserRepository;
import com.corems.userms.app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.verification.email.expiration-hours:24}")
    private int emailExpirationHours;

    @Value("${app.verification.sms.expiration-minutes:10}")
    private int smsExpirationMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate and send email verification token
     */
    @Transactional
    public void sendEmailVerification(UserEntity user) {
        markAllTokensAsUsedForUser(user.getUuid(), VerificationType.EMAIL);
        
        String token = generateEmailToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(emailExpirationHours);
        
        VerificationTokenEntity verificationToken = VerificationTokenEntity.builder()
                .user(user)
                .token(token)
                .type(VerificationType.EMAIL)
                .expiresAt(expiresAt)
                .build();
        
        verificationTokenRepository.save(verificationToken);
        notificationService.sendEmailVerificationCode(user.getEmail(), user.getFirstName(), token);
        
        log.info("Email verification sent to user: {}", user.getEmail());
    }

    /**
     * Generate and send SMS verification code
     */
    @Transactional
    public void sendSmsVerification(UserEntity user) {
        if (user.getPhoneNumber() == null) {
            throw new IllegalArgumentException("User has no phone number");
        }
        
        markAllTokensAsUsedForUser(user.getUuid(), VerificationType.SMS);
        
        String code = generateSmsCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(smsExpirationMinutes);
        
        VerificationTokenEntity verificationToken = VerificationTokenEntity.builder()
                .user(user)
                .token(code)
                .type(VerificationType.SMS)
                .expiresAt(expiresAt)
                .build();
        
        verificationTokenRepository.save(verificationToken);
        notificationService.sendSmsVerificationCode(user.getPhoneNumber(), user.getFirstName(), code);
        
        log.info("SMS verification sent to user: {}", user.getPhoneNumber());
    }

    /**
     * Verify email token and mark user as verified
     */
    @Transactional
    public boolean verifyEmail(String email, String token) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        UserEntity user = userOpt.get();
        Optional<VerificationTokenEntity> tokenOpt = verificationTokenRepository
                .findByTokenAndTypeAndIsUsedFalse(token, VerificationType.EMAIL);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        VerificationTokenEntity verificationToken = tokenOpt.get();
        
        if (!verificationToken.getUser().getUuid().equals(user.getUuid())) {
            return false;
        }
        
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        verificationToken.setIsUsed(true);
        user.setEmailVerified(true);
        
        verificationTokenRepository.save(verificationToken);
        userRepository.save(user);
        
        log.info("Email verified for user: {}", email);
        return true;
    }

    /**
     * Verify SMS code and mark user as verified
     */
    @Transactional
    public boolean verifyPhone(String phoneNumber, String code) {
        Optional<UserEntity> userOpt = userRepository.findByPhoneNumber(phoneNumber);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        UserEntity user = userOpt.get();
        Optional<VerificationTokenEntity> tokenOpt = verificationTokenRepository
                .findByTokenAndTypeAndIsUsedFalse(code, VerificationType.SMS);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        VerificationTokenEntity verificationToken = tokenOpt.get();
        
        if (!verificationToken.getUser().getUuid().equals(user.getUuid())) {
            return false;
        }
        
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        verificationToken.setIsUsed(true);
        user.setPhoneVerified(true);
        
        verificationTokenRepository.save(verificationToken);
        userRepository.save(user);
        
        log.info("Phone verified for user: {}", phoneNumber);
        return true;
    }

    /**
     * Resend verification based on type
     */
    @Transactional
    public void resendVerification(String email, VerificationType type) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        UserEntity user = userOpt.get();
        
        switch (type) {
            case EMAIL -> {
                if (user.getEmailVerified()) {
                    throw new IllegalStateException("Email already verified");
                }
                sendEmailVerification(user);
            }
            case SMS -> {
                if (user.getPhoneNumber() == null) {
                    throw new IllegalArgumentException("User has no phone number");
                }
                if (Boolean.TRUE.equals(user.getPhoneVerified())) {
                    throw new IllegalStateException("Phone already verified");
                }
                sendSmsVerification(user);
            }
        }
    }

    private String generateEmailToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateSmsCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    private void markAllTokensAsUsedForUser(UUID userUuid, VerificationType type) {
        var tokens = verificationTokenRepository.findByUserUuidAndTypeAndIsUsedFalse(userUuid, type);
        tokens.forEach(token -> token.setIsUsed(true));
        verificationTokenRepository.saveAll(tokens);
    }
}