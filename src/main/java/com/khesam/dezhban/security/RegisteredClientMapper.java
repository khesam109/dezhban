package com.khesam.dezhban.security;

import com.khesam.dezhban.common.ClientAuthenticationType;
import com.khesam.dezhban.common.GrantType;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class RegisteredClientMapper {

    private final ObjectMapper objectMapper;

    public RegisteredClientMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RegisteredClient toRegisteredClient(ClientEntity entity) {
        RegisteredClient.Builder builder = RegisteredClient.withId(Long.toString(entity.getId()))
                .clientId(entity.getClientId())
                .clientIdIssuedAt(entity.getCreatedAt())
                .clientSecret(entity.getSecretHash())
                .clientSecretExpiresAt(entity.getSecretExpiresAt())
                .clientName(entity.getClientId())
                .clientSettings(toClientSettings(entity.getClientSettings()))
                .tokenSettings(TokenSettings.builder().build());

        entity.getAuthenticationMethods().stream()
                .map(this::toClientAuthenticationMethod)
                .forEach(builder::clientAuthenticationMethod);
        entity.getGrantTypes().stream()
                .map(this::toAuthorizationGrantType)
                .forEach(builder::authorizationGrantType);
        entity.getRedirectUris().forEach(builder::redirectUri);
        entity.getScopes().forEach(builder::scope);

        if (entity.getPostLogoutRedirectUri() != null) {
            builder.postLogoutRedirectUri(entity.getPostLogoutRedirectUri());
        }

        return builder.build();
    }

    private ClientSettings toClientSettings(String json) {
        try {
            JsonNode settings = objectMapper.readTree(json);
            return ClientSettings.builder()
                    .requireProofKey(settings.path("requireProofKey").asBoolean(false))
                    .requireAuthorizationConsent(
                            settings.path("requireAuthorizationConsent").asBoolean(false)
                    )
                    .build();
        } catch (JacksonException exception) {
            throw new IllegalStateException("Invalid client settings JSON", exception);
        }
    }

    private ClientAuthenticationMethod toClientAuthenticationMethod(
            ClientAuthenticationType authenticationType
    ) {
        return switch (authenticationType) {
            case CLIENT_SECRET_BASIC -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            case CLIENT_SECRET_POST -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
            case CLIENT_SECRET_JWT -> ClientAuthenticationMethod.CLIENT_SECRET_JWT;
            case PRIVATE_KEY_JWT -> ClientAuthenticationMethod.PRIVATE_KEY_JWT;
            case NONE -> ClientAuthenticationMethod.NONE;
        };
    }

    private AuthorizationGrantType toAuthorizationGrantType(GrantType grantType) {
        return switch (grantType) {
            case AUTHORIZATION_CODE -> AuthorizationGrantType.AUTHORIZATION_CODE;
            case REFRESH_TOKEN -> AuthorizationGrantType.REFRESH_TOKEN;
            case CLIENT_CREDENTIALS -> AuthorizationGrantType.CLIENT_CREDENTIALS;
            case JWT_BEARER -> AuthorizationGrantType.JWT_BEARER;
            case DEVICE_CODE -> AuthorizationGrantType.DEVICE_CODE;
            case TOKEN_EXCHANGE -> AuthorizationGrantType.TOKEN_EXCHANGE;
        };
    }
}
