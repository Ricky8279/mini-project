package com.example.miniproject.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

@Configuration
public class JwtConfig {

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Bean
    public KeyPair keyPair() {
        return Keys.keyPairFor(SignatureAlgorithm.ES256);
    }

    @Bean
    public Long getJwtExpiration(){
        return jwtExpiration;
    }
}
