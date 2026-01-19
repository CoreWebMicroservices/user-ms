package com.corems.userms.client;

import com.corems.userms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class UserMsClientConfig {

    @Value("${userms.base-url:http://localhost:3000}")
    private String userMsBaseUrl;

    @Bean(name = "userRestClient")
    @ConditionalOnMissingBean(name = "userRestClient")
    public RestClient userRestClient(RestClient.Builder inboundRestClientBuilder) {
        return inboundRestClientBuilder
                .baseUrl(userMsBaseUrl)
                .build();
    }

    @Bean(name = "userApiClient")
    @ConditionalOnMissingBean(name = "userApiClient")
    public ApiClient userApiClient(RestClient userRestClient) {
        ApiClient apiClient = new ApiClient(userRestClient);
        return apiClient;
    }

    @Bean
    @ConditionalOnMissingBean(name = "adminApi")
    public AdminApi adminApi(ApiClient userApiClient) throws Exception {
       return new AdminApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "profileApi")
    public ProfileApi profileApi(ApiClient userApiClient) throws Exception {
        return new ProfileApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "registrationApi")
    public RegistrationApi registrationApi(ApiClient userApiClient) throws Exception {
        return new RegistrationApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "oauth2Api")
    public OAuth2Api oauth2Api(ApiClient userApiClient) throws Exception {
        return new OAuth2Api(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "oidcApi")
    public OidcApi oidcApi(ApiClient userApiClient) throws Exception {
        return new OidcApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "passwordApi")
    public PasswordApi passwordApi(ApiClient userApiClient) throws Exception {
        return new PasswordApi(userApiClient);
    }

}
