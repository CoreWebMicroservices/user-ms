package com.corems.userms.app.controller;

import com.corems.userms.api.OAuth2Api;
import com.corems.userms.api.model.OAuth2TokenResponse;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.app.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuth2Controller implements OAuth2Api {

    private final OAuth2Service oauth2Service;

    @Override
    public ResponseEntity<Void> authorize(
            String responseType,
            String clientId,
            URI redirectUri,
            String state,
            String codeChallenge,
            String codeChallengeMethod,
            Optional<String> scope,
            Optional<String> nonce) {
        
        String redirectUrl = oauth2Service.handleAuthorize(
            responseType, clientId, redirectUri.toString(), 
            scope.orElse(null), state, codeChallenge, 
            codeChallengeMethod, nonce.orElse(null)
        );
        
        return ResponseEntity.status(302)
            .header("Location", redirectUrl)
            .build();
    }

    @Override
    public ResponseEntity<OAuth2TokenResponse> token(
            String grantType,
            Optional<String> username,
            Optional<String> password,
            Optional<String> code,
            Optional<String> redirectUri,
            Optional<String> codeVerifier,
            Optional<String> refreshToken,
            Optional<String> scope,
            Optional<String> clientId) {
        
        OAuth2TokenResponse response = switch (grantType) {
            case "password" -> oauth2Service.handlePasswordGrant(
                username.orElse(null),
                password.orElse(null),
                scope.orElse(null)
            );
            case "authorization_code" -> oauth2Service.handleAuthorizationCodeGrant(
                code.orElse(null),
                redirectUri.orElse(null),
                codeVerifier.orElse(null),
                clientId.orElse(null)
            );
            case "refresh_token" -> oauth2Service.handleRefreshTokenGrant(
                refreshToken.orElse(null)
            );
            default -> throw new IllegalArgumentException("Unsupported grant_type: " + grantType);
        };
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SuccessfulResponse> revokeToken(
            String token,
            Optional<String> tokenTypeHint) {
        
        oauth2Service.revokeToken(token, tokenTypeHint.orElse(null));
        return ResponseEntity.ok(new SuccessfulResponse().result(true));
    }
}
