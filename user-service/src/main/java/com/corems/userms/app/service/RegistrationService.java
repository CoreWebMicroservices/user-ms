package com.corems.userms.app.service;

import com.corems.userms.api.model.SignUpRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.model.enums.AuthProvider;
import com.corems.userms.app.model.enums.UserActionType;
import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;
import com.corems.userms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserActionTokenService userActionTokenService;
    private final RoleService roleService;

    @Transactional
    public SuccessfulResponse signUp(SignUpRequest signUpRequest) {
        log.info("Sign up request for email: {}", signUpRequest.getEmail());
        
        if (!Objects.equals(signUpRequest.getPassword(), signUpRequest.getConfirmPassword())) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, 
                "Password confirmation does not match"
            );
        }

        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.USER_EXISTS, 
                "User already exists"
            );
        }

        if (signUpRequest.getPhoneNumber() != null) {
            userRepository.findByPhoneNumber(signUpRequest.getPhoneNumber())
                .ifPresent(existingUser -> {
                    throw new AuthServiceException(
                        AuthExceptionReasonCodes.USER_EXISTS, 
                        "Phone number already in use"
                    );
                });
        }

        UserEntity.UserEntityBuilder userBuilder = UserEntity.builder()
                .email(signUpRequest.getEmail())
                .firstName(signUpRequest.getFirstName())
                .lastName(signUpRequest.getLastName())
                .provider(AuthProvider.local.name())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .emailVerified(false);

        if (signUpRequest.getPhoneNumber() != null) {
            userBuilder.phoneNumber(signUpRequest.getPhoneNumber())
                      .phoneVerified(false);
        }

        if (signUpRequest.getImageUrl() != null) {
            userBuilder.imageUrl(signUpRequest.getImageUrl());
        }

        UserEntity user = userBuilder.build();
        roleService.assignDefaultRoles(user);

        UserEntity savedUser = userRepository.save(user);

        userActionTokenService.sendEmailVerification(savedUser);
        
        if (savedUser.getPhoneNumber() != null) {
            userActionTokenService.sendSmsVerification(savedUser);
        }

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse verifyEmail(String email, String token) {
        boolean verified = userActionTokenService.verifyEmail(email, token);
        if (!verified) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_TOKEN, 
                "Invalid or expired verification token"
            );
        }
        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse verifyPhone(String phoneNumber, String code) {
        boolean verified = userActionTokenService.verifyPhone(phoneNumber, code);
        if (!verified) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_TOKEN, 
                "Invalid or expired verification code"
            );
        }
        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse resendVerification(String email, String type) {
        try {
            UserActionType actionType = "EMAIL".equals(type) ? 
                UserActionType.EMAIL_VERIFICATION :
                UserActionType.SMS_VERIFICATION;
            
            userActionTokenService.resendVerification(email, actionType);
            return new SuccessfulResponse().result(true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST, 
                e.getMessage()
            );
        }
    }
}
