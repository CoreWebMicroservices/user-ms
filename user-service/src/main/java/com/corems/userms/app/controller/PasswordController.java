package com.corems.userms.app.controller;

import com.corems.userms.api.PasswordApi;
import com.corems.userms.api.model.ForgotPasswordRequest;
import com.corems.userms.api.model.ResetPasswordRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.app.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PasswordController implements PasswordApi {

    private final PasswordService passwordService;

    @Override
    public ResponseEntity<SuccessfulResponse> forgotPassword(ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordService.forgotPassword(request.getEmail()));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> resetPassword(ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordService.resetPassword(
            request.getEmail(),
            request.getToken(),
            request.getNewPassword(),
            request.getConfirmPassword()
        ));
    }
}
