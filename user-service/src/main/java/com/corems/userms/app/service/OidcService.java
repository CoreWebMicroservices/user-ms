package com.corems.userms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import com.corems.common.security.config.CoremsJwtProperties;
import com.corems.common.security.service.TokenProvider;
import com.corems.userms.api.model.JwksResponse;
import com.corems.userms.api.model.JwksResponseKeysInner;
import com.corems.userms.api.model.OidcConfiguration;
import com.corems.userms.api.model.OidcUserInfo;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OidcService {
    
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final CoremsJwtProperties jwtProperties;

    public OidcConfiguration getDiscoveryDocument() {
        String issuer = jwtProperties.getIssuer();
        
        if (issuer == null) {
            throw new IllegalStateException("JWT issuer not configured in spring.security.jwt.issuer");
        }
        
        String algorithm = tokenProvider.getAlgorithm();
        
        return new OidcConfiguration()
                .issuer(issuer)
                .authorizationEndpoint(issuer + "/oauth2/authorize")
                .tokenEndpoint(issuer + "/oauth2/token")
                .userinfoEndpoint(issuer + "/oauth2/userinfo")
                .jwksUri(issuer + "/.well-known/jwks.json")
                .registrationEndpoint(issuer + "/api/auth/signup")
                .revocationEndpoint(issuer + "/oauth2/revoke")
                .scopesSupported(List.of("openid", "profile", "email", "phone"))
                .responseTypesSupported(List.of("code", "token"))
                .grantTypesSupported(List.of("password", "authorization_code", "refresh_token"))
                .subjectTypesSupported(List.of("public"))
                .idTokenSigningAlgValuesSupported(List.of(algorithm))
                .tokenEndpointAuthMethodsSupported(List.of("client_secret_post", "client_secret_basic", "none"))
                .codeChallengeMethodsSupported(List.of("S256", "plain"));
    }

    public JwksResponse getJwks() {
        PublicKey publicKey = tokenProvider.getPublicKey();
        
        if (publicKey == null) {
            log.warn("No public key available for JWKS");
            return new JwksResponse().keys(List.of());
        }
        
        if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
            log.warn("Public key is not RSA type, cannot generate JWKS");
            return new JwksResponse().keys(List.of());
        }
        
        JwksResponseKeysInner key = new JwksResponseKeysInner()
                .kty("RSA")
                .use("sig")
                .kid(tokenProvider.getKeyId())
                .alg(tokenProvider.getAlgorithm())
                .n(base64UrlEncode(rsaPublicKey.getModulus()))
                .e(base64UrlEncode(rsaPublicKey.getPublicExponent()));
        
        return new JwksResponse().keys(List.of(key));
    }

    public OidcUserInfo getUserInfo() {
        UserPrincipal principal = SecurityUtils.getUserPrincipal();
        
        UserEntity user = userRepository.findByUuid(principal.getUserId())
                .orElseThrow(() -> ServiceException.of(
                    DefaultExceptionReasonCodes.NOT_FOUND,
                    "User not found"
                ));

        return mapToOidcUserInfo(user);
    }
    
    private String base64UrlEncode(BigInteger value) {
        byte[] bytes = value.toByteArray();
        
        if (bytes[0] == 0 && bytes.length > 1) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private OidcUserInfo mapToOidcUserInfo(UserEntity user) {
        OidcUserInfo userInfo = new OidcUserInfo()
                .sub(user.getUuid())
                .email(user.getEmail())
                .emailVerified(user.getEmailVerified())
                .phoneNumber(user.getPhoneNumber())
                .phoneNumberVerified(user.getPhoneVerified())
                .givenName(user.getFirstName())
                .familyName(user.getLastName())
                .updatedAt((int) (user.getUpdatedAt().toEpochMilli() / 1000))
                .roles(user.getRoles().stream()
                    .map(role -> role.getName())
                    .toList())
                .provider(user.getProvider());
        
        if (user.getImageUrl() != null) {
            try {
                userInfo.picture(java.net.URI.create(user.getImageUrl()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid image URL: {}", user.getImageUrl());
            }
        }
        
        return userInfo;
    }
}
