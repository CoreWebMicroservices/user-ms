package com.corems.userms.app.controller;

import com.corems.userms.api.RegistrationApi;
import com.corems.userms.api.model.ResendVerificationRequest;
import com.corems.userms.api.model.SignUpRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.api.model.VerifyEmailRequest;
import com.corems.userms.api.model.VerifyPhoneRequest;
import com.corems.userms.app.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RegistrationController implements RegistrationApi {

    private final RegistrationService registrationService;

    @Override
    public ResponseEntity<SuccessfulResponse> signUp(SignUpRequest signUpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(registrationService.signUp(signUpRequest));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> verifyEmail(VerifyEmailRequest verifyEmailRequest) {
        return ResponseEntity.ok(registrationService.verifyEmail(
            verifyEmailRequest.getEmail(), 
            verifyEmailRequest.getToken()
        ));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> verifyPhone(VerifyPhoneRequest verifyPhoneRequest) {
        return ResponseEntity.ok(registrationService.verifyPhone(
            verifyPhoneRequest.getPhoneNumber(), 
            verifyPhoneRequest.getCode()
        ));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> resendVerification(ResendVerificationRequest request) {
        return ResponseEntity.ok(registrationService.resendVerification(
            request.getEmail(), 
            request.getType().getValue()
        ));
    }
}
