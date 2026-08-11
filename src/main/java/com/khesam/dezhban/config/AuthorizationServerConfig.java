package com.khesam.dezhban.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
class AuthorizationServerConfig {

    @Bean
    @Order(1)
    SecurityFilterChain authServerConfigurer(HttpSecurity http) throws Exception {
        http.oauth2AuthorizationServer(authServerConfigurer -> {
            http.securityMatcher(authServerConfigurer.getEndpointsMatcher());
            authServerConfigurer.oidc(Customizer.withDefaults());
        });

        http.authorizeHttpRequests(requests ->
                requests.anyRequest().authenticated()
        );

        http.exceptionHandling(e ->
                e.authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint("/login")
                )
        );

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests ->
                requests.requestMatchers("/error", "/favicon.ico", "/callback.html").permitAll()
                .anyRequest().authenticated()
        );

        http.formLogin(Customizer.withDefaults());


        return http.build();
    }

    @Bean
    AuthorizationServerSettings settings() {
        return AuthorizationServerSettings.builder().build();
    }
}
