package com.corems.userms.app.controller;

import com.corems.userms.api.ProfileApi;
import com.corems.userms.api.model.ChangePasswordRequest;
import com.corems.userms.api.model.OidcUserInfo;
import com.corems.userms.api.model.ProfileUpdateRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.app.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;

    @Override
    public ResponseEntity<OidcUserInfo> updateProfile(ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> changePassword(ChangePasswordRequest request) {
        return ResponseEntity.ok(profileService.changePassword(request));
    }
}
