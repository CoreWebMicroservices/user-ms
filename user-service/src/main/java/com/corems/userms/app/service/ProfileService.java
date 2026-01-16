package com.corems.userms.app.service;

import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import com.corems.userms.api.model.ChangePasswordRequest;
import com.corems.userms.api.model.OidcUserInfo;
import com.corems.userms.api.model.ProfileUpdateRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.model.enums.AuthProvider;
import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;
import com.corems.userms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private static final String USER_NOT_FOUND_MSG = "User id: %s not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OidcService oidcService;

    public OidcUserInfo updateProfile(ProfileUpdateRequest request) {
        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();
        UserEntity user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(
                    AuthExceptionReasonCodes.USER_NOT_FOUND,
                    String.format(USER_NOT_FOUND_MSG, userPrincipal.getUserId())
                ));
        
        if (request.getPhoneNumber() != null 
            && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            
            userRepository.findByPhoneNumber(request.getPhoneNumber())
                .ifPresent(existingUser -> {
                    if (!existingUser.getUuid().equals(user.getUuid())) {
                        throw new AuthServiceException(
                            AuthExceptionReasonCodes.USER_EXISTS, 
                            "Phone number already in use"
                        );
                    }
                });
        }
        
        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getImageUrl() != null)
            user.setImageUrl(request.getImageUrl());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
            user.setPhoneVerified(false);
        }
        
        userRepository.save(user);
        return oidcService.getUserInfo();
    }

    public SuccessfulResponse changePassword(ChangePasswordRequest request) {
        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();
        UserEntity user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(
                    AuthExceptionReasonCodes.USER_NOT_FOUND,
                    String.format(USER_NOT_FOUND_MSG, userPrincipal.getUserId())
                ));

        if (user.getPassword() != null
                && !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, 
                "Wrong password"
            );
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH,
                "Password confirmation does not match"
            );
        }

        if (!user.getProvider().contains(AuthProvider.local.name())) {
            user.setProvider(user.getProvider() + "," + AuthProvider.local.name());
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        return new SuccessfulResponse().result(true);
    }
}
