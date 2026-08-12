package com.khesam.dezhban.security;

import com.khesam.dezhban.controller.dto.TokenErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class TokenErrorResponseHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    public TokenErrorResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        OAuth2Error error = exception instanceof OAuth2AuthenticationException oauthException
                ? oauthException.getError()
                : new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR);
        boolean invalidClient = OAuth2ErrorCodes.INVALID_CLIENT.equals(error.getErrorCode());

        response.setStatus(invalidClient
                ? HttpServletResponse.SC_UNAUTHORIZED
                : HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        if (invalidClient && request.getHeader(HttpHeaders.AUTHORIZATION) != null
                && request.getHeader(HttpHeaders.AUTHORIZATION).regionMatches(
                    true, 0, "Basic ", 0, 6
                )) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic");
        }

        objectMapper.writeValue(
                response.getOutputStream(),
                new TokenErrorResponseDto(
                        error.getErrorCode(),
                        error.getDescription(),
                        error.getUri()
                )
        );
    }
}
