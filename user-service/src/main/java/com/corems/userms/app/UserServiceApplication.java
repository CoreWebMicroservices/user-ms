package com.corems.userms.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.corems.common.security.config.CoremsJwtProperties;
import com.corems.common.security.service.TokenProvider;

@EnableConfigurationProperties(CoremsJwtProperties.class)
@SpringBootApplication
public class UserServiceApplication {

    @Bean
    public TokenProvider tokenProvider(CoremsJwtProperties jwtProperties) {
        return new TokenProvider(jwtProperties);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}