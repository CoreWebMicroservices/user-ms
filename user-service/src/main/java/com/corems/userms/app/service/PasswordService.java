package com.corems.userms.app.service;

import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PasswordService {
    
    private final UserActionTokenService userActionTokenService;

    public SuccessfulResponse forgotPassword(String email) {
        userActionTokenService.sendPasswordResetEmail(email);
        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse resetPassword(
            String email, 
            String token, 
            String newPassword, 
            String confirmPassword) {
        
        if (!Objects.equals(newPassword, confirmPassword)) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, 
                "Password confirmation does not match"
            );
        }

        boolean success = userActionTokenService.resetPassword(email, token, newPassword);
        if (!success) {
            throw new AuthServiceException(AuthExceptionReasonCodes.INVALID_TOKEN);
        }

        return new SuccessfulResponse().result(true);
    }
}
