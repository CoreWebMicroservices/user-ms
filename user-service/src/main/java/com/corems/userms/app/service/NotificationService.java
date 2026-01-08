package com.corems.userms.app.service;

import com.corems.communicationms.api.model.EmailNotificationRequest;
import com.corems.communicationms.api.model.SmsNotificationRequest;
import com.corems.communicationms.client.NotificationsApi;
import com.corems.userms.app.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationsApi notificationsApi;

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
            EmailNotificationRequest request = new EmailNotificationRequest();
            request.setSubject("Verify Your Email Address");
            request.setRecipient(email);
            request.setBody("Dear " + firstName + ",\n\n" +
                    "Please verify your email address by using the following verification code:\n\n" +
                    "Verification Code: " + token + "\n\n" +
                    "This code will expire in 24 hours.\n\n" +
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
}
