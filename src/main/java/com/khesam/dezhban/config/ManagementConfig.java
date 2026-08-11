package com.khesam.dezhban.config;

import com.khesam.dezhban.security.KeycloakPbkdf2PasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@Configuration
class ManagementConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        String defaultEncodingId = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        // New user passwords and imported Keycloak client secrets are re-hashed with BCrypt.
        encoders.put(defaultEncodingId, new BCryptPasswordEncoder(12));
        // Existing Keycloak user hashes remain verifiable during migration.
        encoders.put("keycloak-pbkdf2", new KeycloakPbkdf2PasswordEncoder());

        return new DelegatingPasswordEncoder(defaultEncodingId, encoders);
    }
}
