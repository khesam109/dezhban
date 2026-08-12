package com.khesam.dezhban.security;

import com.khesam.dezhban.controller.dto.TokenResponseDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Set;

@Component
public class TokenResponseHandler implements AuthenticationSuccessHandler {

    private static final Set<String> RESERVED_PARAMETERS = Set.of(
            OAuth2ParameterNames.ACCESS_TOKEN,
            OAuth2ParameterNames.TOKEN_TYPE,
            OAuth2ParameterNames.EXPIRES_IN,
            OAuth2ParameterNames.REFRESH_TOKEN,
            OAuth2ParameterNames.SCOPE
    );

    private final ObjectMapper objectMapper;

    public TokenResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AccessTokenAuthenticationToken tokenAuthentication)) {
            throw new ServletException("Unexpected token endpoint authentication result");
        }

        var accessToken = tokenAuthentication.getAccessToken();
        var refreshToken = tokenAuthentication.getRefreshToken();
        long expiresIn = ChronoUnit.SECONDS.between(
                accessToken.getIssuedAt(),
                accessToken.getExpiresAt()
        );
        String scopes = accessToken.getScopes().isEmpty()
                ? null
                : String.join(" ", accessToken.getScopes());
        HashMap<String, Object> additionalParameters =
                new HashMap<>(tokenAuthentication.getAdditionalParameters());
        RESERVED_PARAMETERS.forEach(additionalParameters::remove);

        TokenResponseDto body = new TokenResponseDto(
                accessToken.getTokenValue(),
                accessToken.getTokenType().getValue(),
                expiresIn,
                refreshToken == null ? null : refreshToken.getTokenValue(),
                scopes,
                additionalParameters
        );

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
