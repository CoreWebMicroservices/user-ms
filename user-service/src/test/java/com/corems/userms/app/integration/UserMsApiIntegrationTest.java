package com.corems.userms.app.integration;

import com.corems.userms.ApiClient;
import com.corems.userms.api.model.*;
import com.corems.userms.client.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientResponseException;



import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private RegistrationApi registrationApi;
    @Autowired
    private OAuth2Api oauth2Api;
    @Autowired
    private OidcApi oidcApi;
    @Autowired
    private ProfileApi profileApi;
    @Autowired
    private AdminApi adminApi;

    private SignUpRequest signUpRequest;
    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        apiClient.setBasePath("http://localhost:" + port);
        
        testEmail = "testuser" + System.currentTimeMillis() + "@example.com";
        testPassword = "TestPassword123!";
        
        signUpRequest = new SignUpRequest();
        signUpRequest.setEmail(testEmail);
        signUpRequest.setPassword(testPassword);
        signUpRequest.setConfirmPassword(testPassword);
        signUpRequest.setFirstName("Test");
        signUpRequest.setLastName("User");
    }

    private OAuth2TokenResponse createUserAndAuthenticate() {
        registrationApi.signUp(signUpRequest);
        
        OAuth2TokenResponse tokenResponse = oauth2Api.token(
            "password",
            testEmail,
            testPassword,
            null, null, null, null,
            "openid profile email",
            null
        );
        
        apiClient.setBearerToken(tokenResponse.getAccessToken());
        return tokenResponse;
    }

    // ==================== Public Endpoints ====================

    @Test
    @Order(1)
    @DirtiesContext
    void signUp_WhenValidRequest_ShouldCreateUser() {
        SuccessfulResponse response = registrationApi.signUp(signUpRequest);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();
    }

    @Test
    @Order(2)
    void oauth2Token_WhenValidPasswordGrant_ShouldReturnTokens() {
        registrationApi.signUp(signUpRequest);

        OAuth2TokenResponse response = oauth2Api.token(
            "password",
            testEmail,
            testPassword,
            null, null, null, null,
            "openid profile email",
            null
        );

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getTokenType()).isEqualTo(OAuth2TokenResponse.TokenTypeEnum.BEARER);
        assertThat(response.getIdToken()).isNotNull();
    }

    @Test
    @Order(3)
    void oauth2Token_WhenInvalidCredentials_ShouldThrowException() {
        assertThatThrownBy(() -> oauth2Api.token(
            "password",
            "nonexistent@example.com",
            "wrongpassword",
            null, null, null, null,
            "openid profile email",
            null
        ))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(4)
    void signUp_WhenDuplicateEmail_ShouldThrowException() {
        registrationApi.signUp(signUpRequest);

        assertThatThrownBy(() -> registrationApi.signUp(signUpRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    // ==================== Protected Endpoints ====================

    @Test
    @Order(10)
    @DirtiesContext
    void getUserInfo_WhenAuthenticated_ShouldReturnUserInfo() {
        createUserAndAuthenticate();

        OidcUserInfo userInfo = oidcApi.getUserInfo();

        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getEmail()).isEqualTo(testEmail);
        assertThat(userInfo.getGivenName()).isEqualTo("Test");
        assertThat(userInfo.getFamilyName()).isEqualTo("User");
        assertThat(userInfo.getSub()).isNotNull();
    }

    @Test
    @Order(11)
    @DirtiesContext
    void updateProfile_WhenAuthenticated_ShouldUpdateProfile() {
        createUserAndAuthenticate();

        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setPhoneNumber("+1234567890");

        OidcUserInfo updatedUserInfo = profileApi.updateProfile(updateRequest);

        assertThat(updatedUserInfo).isNotNull();
        assertThat(updatedUserInfo.getGivenName()).isEqualTo("Updated");
        assertThat(updatedUserInfo.getFamilyName()).isEqualTo("Name");
        assertThat(updatedUserInfo.getPhoneNumber()).isEqualTo("+1234567890");
    }

    @Test
    @Order(12)
    @DirtiesContext
    void changePassword_WhenAuthenticated_ShouldChangePassword() {
        createUserAndAuthenticate();

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword(testPassword);
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");

        SuccessfulResponse response = profileApi.changePassword(changePasswordRequest);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();

        assertThatThrownBy(() -> oauth2Api.token(
            "password",
            testEmail,
            testPassword,
            null, null, null, null,
            "openid profile email",
            null
        ))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));

        OAuth2TokenResponse newTokenResponse = oauth2Api.token(
            "password",
            testEmail,
            "NewPassword123!",
            null, null, null, null,
            "openid profile email",
            null
        );
        assertThat(newTokenResponse.getAccessToken()).isNotNull();
    }

    @Test
    @Order(13)
    @DirtiesContext
    void refreshToken_WhenValidRefreshToken_ShouldReturnNewAccessToken() {
        OAuth2TokenResponse tokenResponse = createUserAndAuthenticate();
        
        apiClient.setBearerToken((String) null);

        OAuth2TokenResponse response = oauth2Api.token(
            "refresh_token",
            null, null, null, null, null,
            tokenResponse.getRefreshToken(),
            null, null
        );

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getAccessToken()).isNotEqualTo(tokenResponse.getAccessToken());
    }

    @Test
    @Order(14)
    @DirtiesContext
    void revokeToken_WhenAuthenticated_ShouldRevokeToken() {
        OAuth2TokenResponse tokenResponse = createUserAndAuthenticate();

        SuccessfulResponse response = oauth2Api.revokeToken(tokenResponse.getRefreshToken(), "refresh_token");

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();
    }

    @Test
    @Order(20)
    @DirtiesContext
    void getAllUsers_WhenAuthenticated_ShouldReturnUsersList() {
        createUserAndAuthenticate();

        UsersPagedResponse response = adminApi.getAllUsers(1, 10, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getTotalElements()).isGreaterThan(0);
    }

    @Test
    @Order(21)
    @DirtiesContext
    void getUserById_WhenValidId_ShouldReturnUser() {
        createUserAndAuthenticate();
        OidcUserInfo currentUser = oidcApi.getUserInfo();

        UserInfo userInfo = adminApi.getUserById(currentUser.getSub());

        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getEmail()).isEqualTo(testEmail);
    }

    @Test
    @Order(30)
    void apiCalls_WhenNotAuthenticated_ShouldReturn401() {
        assertThatThrownBy(() -> oidcApi.getUserInfo())
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));

        assertThatThrownBy(() -> adminApi.getAllUsers(1, 10, null, null, null))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }
}
