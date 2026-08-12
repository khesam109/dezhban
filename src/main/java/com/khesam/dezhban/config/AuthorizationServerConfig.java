package com.khesam.dezhban.config;

import com.khesam.dezhban.security.TokenErrorResponseHandler;
import com.khesam.dezhban.security.TokenResponseHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
class AuthorizationServerConfig {

    @Bean
    @Order(1)
    SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/h2-console/**");
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain authServerConfigurer(
            HttpSecurity http,
            TokenResponseHandler tokenResponseHandler,
            TokenErrorResponseHandler tokenErrorResponseHandler
    ) throws Exception {
        http.oauth2AuthorizationServer(authServerConfigurer -> {
            http.securityMatcher(authServerConfigurer.getEndpointsMatcher());
            authServerConfigurer.oidc(Customizer.withDefaults());
            authServerConfigurer.tokenEndpoint(tokenEndpoint -> tokenEndpoint
                    .accessTokenResponseHandler(tokenResponseHandler)
                    .errorResponseHandler(tokenErrorResponseHandler)
            );
            authServerConfigurer.clientAuthentication(clientAuthentication ->
                    clientAuthentication.errorResponseHandler(tokenErrorResponseHandler)
            );
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
    @Order(3)
    SecurityFilterChain adminApiSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http.securityMatcher("/api/v1/admin/**");
        http.authorizeHttpRequests(requests ->
                requests.anyRequest().hasRole("ADMIN")
        );
        http.httpBasic(Customizer.withDefaults());
        http.oauth2ResourceServer(resourceServer ->
                resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
        );
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
        return http.build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http.authorizeHttpRequests(requests ->
                requests.requestMatchers(
                        "/error",
                        "/favicon.ico",
                        "/callback.html",
                        "/api/v1/webauthn/authentication/**"
                ).permitAll()
                .anyRequest().authenticated()
        );

        http.formLogin(Customizer.withDefaults());
        http.oauth2ResourceServer(resourceServer ->
                resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
        );

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeAuthorities = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new java.util.ArrayList<>(scopeAuthorities.convert(jwt));
            var roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    AuthorizationServerSettings settings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationConsentService(
                jdbcOperations,
                registeredClientRepository
        );
    }
}
