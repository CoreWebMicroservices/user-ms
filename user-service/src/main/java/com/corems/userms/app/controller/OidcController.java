package com.corems.userms.app.controller;

import com.corems.userms.api.OidcApi;
import com.corems.userms.api.model.JwksResponse;
import com.corems.userms.api.model.OidcConfiguration;
import com.corems.userms.api.model.OidcUserInfo;
import com.corems.userms.app.service.OidcService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OidcController implements OidcApi {

    private final OidcService oidcService;

    @Override
    public ResponseEntity<OidcConfiguration> getOidcConfiguration() {
        return ResponseEntity.ok(oidcService.getDiscoveryDocument());
    }

    @Override
    public ResponseEntity<JwksResponse> getJwks() {
        return ResponseEntity.ok(oidcService.getJwks());
    }

    @Override
    public ResponseEntity<OidcUserInfo> getUserInfo() {
        return ResponseEntity.ok(oidcService.getUserInfo());
    }
}
