package com.corems.userms.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.corems.common.security.config.CoremsJwtProperties;
import com.corems.common.security.config.CoreMsPermitAllSecurityConfig;
import com.corems.common.security.config.CoreMsSecurityConfig;
import com.corems.common.security.service.TokenProvider;

@EnableConfigurationProperties(CoremsJwtProperties.class)
@SpringBootApplication(exclude = {CoreMsSecurityConfig.class, CoreMsPermitAllSecurityConfig.class})
public class UserServiceApplication {

    @Bean
    public TokenProvider tokenProvider(CoremsJwtProperties jwtProperties) {
        return new TokenProvider(jwtProperties);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}