package com.corems.userms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.UserPrincipal;
import com.corems.common.security.service.TokenProvider;
import com.corems.userms.api.model.OAuth2TokenResponse;
import com.corems.userms.app.entity.LoginTokenEntity;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.app.repository.LoginTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final TokenProvider tokenProvider;
    private final LoginTokenRepository loginTokenRepository;

    public OAuth2TokenResponse generateTokenResponse(UserEntity user, String scope, String nonce) {
        String refreshToken = createRefreshToken(user);
        String accessToken = tokenProvider.createAccessToken(
            user.getUuid().toString(), 
            getAccessTokenClaims(user)
        );
        
        String idToken = null;
        if (scope != null && scope.contains("openid")) {
            idToken = generateIdToken(user, scope, nonce);
        }

        return new OAuth2TokenResponse()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idToken(idToken)
                .tokenType(OAuth2TokenResponse.TokenTypeEnum.BEARER)
                .expiresIn(600)
                .scope(scope != null ? scope : "openid profile email");
    }
    
    public String generateIdToken(UserEntity user, String scope, String nonce) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        
        claims.put("auth_time", now.getEpochSecond());
        
        if (nonce != null) {
            claims.put("nonce", nonce);
        }
        
        if (scope != null) {
            if (scope.contains("email")) {
                claims.put("email", user.getEmail());
                claims.put("email_verified", user.getEmailVerified());
            }
            
            if (scope.contains("profile")) {
                claims.put("given_name", user.getFirstName());
                claims.put("family_name", user.getLastName());
                claims.put("updated_at", user.getUpdatedAt().getEpochSecond());
                
                if (user.getImageUrl() != null) {
                    claims.put("picture", user.getImageUrl());
                }
            }
            
            if (scope.contains("phone")) {
                if (user.getPhoneNumber() != null) {
                    claims.put("phone_number", user.getPhoneNumber());
                    claims.put("phone_number_verified", user.getPhoneVerified());
                }
            }
        }
        
        return tokenProvider.createIdToken(user.getUuid().toString(), claims, 60);
    }

    public String createRefreshToken(UserEntity user) {
        UUID tokenId = UUID.randomUUID();
        
        Map<String, Object> claims = Map.of(
            TokenProvider.CLAIM_TOKEN_ID, tokenId.toString()
        );
        
        String refreshToken = tokenProvider.createRefreshToken(
            user.getUuid().toString(), 
            claims
        );

        LoginTokenEntity loginToken = new LoginTokenEntity();
        loginToken.setUuid(tokenId);
        loginToken.setUser(user);
        loginToken.setToken(refreshToken);
        loginTokenRepository.save(loginToken);

        return refreshToken;
    }

    public void validateRefreshToken(UserPrincipal userPrincipal) {
        LoginTokenEntity refreshToken = loginTokenRepository
                .findByUuid(userPrincipal.getTokenId())
                .orElseThrow(() -> ServiceException.of(
                    UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, 
                    String.format("Token not found with ID: %s", userPrincipal.getTokenId())
                ));

        if (!Objects.equals(userPrincipal.getUserId(), refreshToken.getUser().getUuid())) {
            throw ServiceException.of(
                UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, 
                String.format("Token not found with ID: %s", userPrincipal.getTokenId())
            );
        }
    }

    private Map<String, Object> getAccessTokenClaims(UserEntity user) {
        return Map.of(
                TokenProvider.CLAIM_EMAIL, user.getEmail(),
                TokenProvider.CLAIM_FIRST_NAME, user.getFirstName(),
                TokenProvider.CLAIM_LAST_NAME, user.getLastName(),
                TokenProvider.CLAIM_ROLES, user.getRoles().stream()
                    .map(RoleEntity::getName)
                    .toList()
        );
    }
}
