package com.corems.userms.app.service;

import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import com.corems.userms.api.model.OAuth2TokenResponse;
import com.corems.userms.app.entity.AuthorizationCodeEntity;
import com.corems.userms.app.entity.LoginTokenEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;
import com.corems.userms.app.repository.AuthorizationCodeRepository;
import com.corems.userms.app.repository.LoginTokenRepository;
import com.corems.userms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    
    private final UserRepository userRepository;
    private final LoginTokenRepository loginTokenRepository;
    private final AuthorizationCodeRepository authorizationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public String handleAuthorize(
            String responseType,
            String clientId,
            String redirectUri,
            String scope,
            String state,
            String codeChallenge,
            String codeChallengeMethod,
            String nonce) {
        
        if (!"code".equals(responseType)) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "Only 'code' response_type is supported"
            );
        }
        
        if (clientId == null || redirectUri == null) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "client_id and redirect_uri are required"
            );
        }
        
        UserPrincipal principal = SecurityUtils.getUserPrincipal();
        UserEntity user = userRepository.findByUuid(principal.getUserId())
                .orElseThrow(() -> new AuthServiceException(
                    AuthExceptionReasonCodes.USER_NOT_FOUND,
                    "User not found"
                ));
        
        String code = UUID.randomUUID().toString();
        
        AuthorizationCodeEntity authCode = new AuthorizationCodeEntity();
        authCode.setCode(code);
        authCode.setUser(user);
        authCode.setClientId(clientId);
        authCode.setRedirectUri(redirectUri);
        authCode.setScope(scope);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setCodeChallengeMethod(codeChallengeMethod);
        authCode.setNonce(nonce);
        authCode.setExpiresAt(Instant.now().plusSeconds(600));
        authCode.setIsUsed(false);
        
        authorizationCodeRepository.save(authCode);
        
        String redirectUrl = redirectUri + "?code=" + code;
        if (state != null) {
            redirectUrl += "&state=" + state;
        }
        
        return redirectUrl;
    }

    public OAuth2TokenResponse handlePasswordGrant(String username, String password, String scope) {
        UserEntity user = userRepository
                .findByEmail(username)
                .orElseThrow(() -> new AuthServiceException(
                    AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, 
                    "Invalid credentials"
                ));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, 
                "Invalid credentials"
            );
        }

        return tokenService.generateTokenResponse(user, scope, null);
    }

    @Transactional
    public OAuth2TokenResponse handleAuthorizationCodeGrant(
            String code,
            String redirectUri,
            String codeVerifier,
            String clientId) {
        
        AuthorizationCodeEntity authCode = authorizationCodeRepository.findByCode(code)
                .orElseThrow(() -> new AuthServiceException(
                    AuthExceptionReasonCodes.INVALID_REQUEST,
                    "Invalid authorization code"
                ));
        
        if (authCode.getIsUsed()) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "Authorization code already used"
            );
        }
        
        if (authCode.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "Authorization code expired"
            );
        }
        
        if (!authCode.getClientId().equals(clientId)) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "client_id mismatch"
            );
        }
        
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "redirect_uri mismatch"
            );
        }
        
        if (authCode.getCodeChallenge() != null) {
            if (codeVerifier == null) {
                throw new AuthServiceException(
                    AuthExceptionReasonCodes.INVALID_REQUEST,
                    "code_verifier required for PKCE"
                );
            }
            
            String computedChallenge = computeCodeChallenge(codeVerifier, authCode.getCodeChallengeMethod());
            if (!computedChallenge.equals(authCode.getCodeChallenge())) {
                throw new AuthServiceException(
                    AuthExceptionReasonCodes.INVALID_REQUEST,
                    "Invalid code_verifier"
                );
            }
        }
        
        authCode.setIsUsed(true);
        authorizationCodeRepository.save(authCode);
        
        return tokenService.generateTokenResponse(authCode.getUser(), authCode.getScope(), authCode.getNonce());
    }
    
    private String computeCodeChallenge(String codeVerifier, String method) {
        if ("plain".equals(method)) {
            return codeVerifier;
        }
        
        if ("S256".equals(method) || method == null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 not available", e);
            }
        }
        
        throw new AuthServiceException(
            AuthExceptionReasonCodes.INVALID_REQUEST,
            "Unsupported code_challenge_method"
        );
    }

    @Transactional
    public OAuth2TokenResponse handleRefreshTokenGrant(String refreshToken) {
        if (refreshToken == null) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "refresh_token is required"
            );
        }
        
        LoginTokenEntity loginToken = loginTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_TOKEN,
                "Invalid refresh token"
            ));
        
        UserEntity user = loginToken.getUser();
        
        return tokenService.generateTokenResponse(user, "openid profile email", null);
    }

    @Transactional
    public void revokeToken(String token, String tokenTypeHint) {
        if (token == null) {
            throw new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_REQUEST,
                "token is required"
            );
        }
        
        LoginTokenEntity loginToken = loginTokenRepository.findByToken(token)
            .orElseThrow(() -> new AuthServiceException(
                AuthExceptionReasonCodes.INVALID_TOKEN,
                "Invalid token"
            ));
        
        log.info("Revoking token for user: {}", loginToken.getUser().getUuid());
        loginTokenRepository.delete(loginToken);
    }
}
