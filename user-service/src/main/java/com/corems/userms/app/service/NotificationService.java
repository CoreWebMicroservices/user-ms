package com.corems.userms.app.service;

import com.corems.communicationms.api.model.EmailNotificationRequest;
import com.corems.communicationms.api.model.SmsNotificationRequest;
import com.corems.communicationms.client.NotificationsApi;
import com.corems.userms.app.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationsApi notificationsApi;
    
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;
    
    private static final String EMAIL_VERIFICATION_PATH = "/verify-email";
    private static final String PASSWORD_RESET_PATH = "/reset-password";

    @Async
    public void sendWelcomeEmail(UserEntity user) {
        try {
            EmailNotificationRequest request = new EmailNotificationRequest();
            request.setSubject("Welcome to CoreMS");
            request.setRecipient(user.getEmail());
            request.setBody("Dear " + user.getFirstName() + ",\n\n" +
                    "Welcome to CoreMS! We're excited to have you on board.\n\n" +
                    "Best regards,\n" +
                    "The CoreMS Team");

            var res = notificationsApi.sendEmailNotification(request);

            log.info("Welcome email sent to user: {}, result: {}", user.getUuid(), res);
        } catch (Exception e) {
            log.error("Failed to send welcome email to user: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendWelcomeSms(UserEntity user) {
        if (user.getPhoneNumber() == null) {
            log.debug("No phone number for user: {}, skipping SMS", user.getUuid());
            return;
        }

        try {
            SmsNotificationRequest request = new SmsNotificationRequest();
            request.setPhoneNumber(user.getPhoneNumber());
            request.setMessage("Welcome to CoreMS, " + user.getFirstName() + "!");

            var res = notificationsApi.sendSmsNotification(request);

            log.info("Welcome SMS sent to user: {}, result: {}", user.getUuid(), res);
        } catch (Exception e) {
            log.error("Failed to send welcome SMS to user: {}", user.getPhoneNumber(), e);
        }
    }

    @Async
    public void sendEmailVerificationCode(String email, String firstName, String token) {
        try {
            String verificationUrl = frontendBaseUrl + EMAIL_VERIFICATION_PATH + "?email=" + email + "&token=" + token;
            
            EmailNotificationRequest request = new EmailNotificationRequest();
            request.setSubject("Verify Your Email Address");
            request.setRecipient(email);
            request.setBody("Dear " + firstName + ",\n\n" +
                    "Please verify your email address by clicking the link below:\n\n" +
                    verificationUrl + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you didn't create an account with us, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "The CoreMS Team");

            var res = notificationsApi.sendEmailNotification(request);

            log.info("Email verification sent to: {}, result: {}", email, res);
        } catch (Exception e) {
            log.error("Failed to send email verification to: {}", email, e);
        }
    }

    @Async
    public void sendSmsVerificationCode(String phoneNumber, String firstName, String code) {
        try {
            SmsNotificationRequest request = new SmsNotificationRequest();
            request.setPhoneNumber(phoneNumber);
            request.setMessage("Hi " + firstName + "! Your CoreMS verification code is: " + code + ". Valid for 10 minutes.");

            var res = notificationsApi.sendSmsNotification(request);

            log.info("SMS verification sent to: {}, result: {}", phoneNumber, res);
        } catch (Exception e) {
            log.error("Failed to send SMS verification to: {}", phoneNumber, e);
        }
    }

    @Async
    public void sendPasswordResetEmail(UserEntity user, String resetToken) {
        try {
            String resetUrl = frontendBaseUrl + PASSWORD_RESET_PATH + "?email=" + user.getEmail() + "&token=" + resetToken;
            
            EmailNotificationRequest request = new EmailNotificationRequest();
            request.setSubject("Password Reset Request");
            request.setRecipient(user.getEmail());
            request.setBody("Dear " + user.getFirstName() + ",\n\n" +
                    "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                    resetUrl + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you didn't request a password reset, please ignore this email and your password will remain unchanged.\n\n" +
                    "Best regards,\n" +
                    "The CoreMS Team");

            var res = notificationsApi.sendEmailNotification(request);

            log.info("Password reset email sent to user: {}, result: {}", user.getUuid(), res);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }
}
