package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.OAuth2AuthorizationEntity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
public class OAuth2AuthorizationMapper {

    private static final String PLACEHOLDER_PREFIX = "urn:dezhban:sha256:";

    private final OAuth2AuthorizationCodec codec;
    private final Sha256TokenHasher tokenHasher;

    public OAuth2AuthorizationMapper(
            OAuth2AuthorizationCodec codec,
            Sha256TokenHasher tokenHasher
    ) {
        this.codec = codec;
        this.tokenHasher = tokenHasher;
    }

    public void updateEntity(
            OAuth2Authorization authorization,
            OAuth2AuthorizationEntity entity
    ) {
        entity.setId(authorization.getId());
        entity.setRegisteredClientId(authorization.getRegisteredClientId());

        String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
        entity.setStateDigest(state == null ? null : tokenHasher.digest(state));
        entity.setAuthorizationCodeDigest(digest(authorization, OAuth2AuthorizationCode.class));
        entity.setAccessTokenDigest(digest(authorization, OAuth2AccessToken.class));
        entity.setOidcIdTokenDigest(digest(authorization, OidcIdToken.class));
        entity.setRefreshTokenDigest(digest(authorization, OAuth2RefreshToken.class));
        entity.setUserCodeDigest(digest(authorization, OAuth2UserCode.class));
        entity.setDeviceCodeDigest(digest(authorization, OAuth2DeviceCode.class));

        entity.setAuthorizationData(codec.encode(withDigestPlaceholders(authorization)));
    }

    public OAuth2Authorization toAuthorization(
            OAuth2AuthorizationEntity entity,
            TokenKind matchedTokenKind,
            String presentedToken
    ) {
        OAuth2Authorization authorization = codec.decode(entity.getAuthorizationData());
        if (matchedTokenKind == null || matchedTokenKind == TokenKind.STATE) {
            return authorization;
        }
        return replaceTokenValue(authorization, matchedTokenKind, presentedToken);
    }

    private OAuth2Authorization withDigestPlaceholders(OAuth2Authorization authorization) {
        OAuth2Authorization sanitized = authorization;
        for (TokenKind tokenKind : TokenKind.values()) {
            if (tokenKind != TokenKind.STATE) {
                sanitized = replaceWithDigestPlaceholder(sanitized, tokenKind);
            }
        }
        return sanitized;
    }

    private OAuth2Authorization replaceWithDigestPlaceholder(
            OAuth2Authorization authorization,
            TokenKind tokenKind
    ) {
        OAuth2Authorization.Token<?> storedToken = authorization.getToken(tokenKind.tokenClass);
        if (storedToken == null) {
            return authorization;
        }
        String digest = digest(storedToken.getToken().getTokenValue(), tokenKind);
        return replaceTokenValue(
                authorization,
                tokenKind,
                PLACEHOLDER_PREFIX + tokenKind.name().toLowerCase() + ":" + digest
        );
    }

    private OAuth2Authorization replaceTokenValue(
            OAuth2Authorization authorization,
            TokenKind tokenKind,
            String tokenValue
    ) {
        OAuth2Authorization.Token<?> storedToken = authorization.getToken(tokenKind.tokenClass);
        if (storedToken == null) {
            return authorization;
        }

        OAuth2Token token = copyTokenWithValue(storedToken.getToken(), tokenKind, tokenValue);
        Consumer<Map<String, Object>> metadata =
                values -> values.putAll(storedToken.getMetadata());
        return OAuth2Authorization.from(authorization)
                .token(token, metadata)
                .build();
    }

    private OAuth2Token copyTokenWithValue(
            OAuth2Token original,
            TokenKind tokenKind,
            String tokenValue
    ) {
        return switch (tokenKind) {
            case AUTHORIZATION_CODE -> new OAuth2AuthorizationCode(
                    tokenValue,
                    original.getIssuedAt(),
                    original.getExpiresAt()
            );
            case ACCESS_TOKEN -> {
                OAuth2AccessToken accessToken = (OAuth2AccessToken) original;
                yield new OAuth2AccessToken(
                        accessToken.getTokenType(),
                        tokenValue,
                        accessToken.getIssuedAt(),
                        accessToken.getExpiresAt(),
                        accessToken.getScopes()
                );
            }
            case OIDC_ID_TOKEN -> {
                OidcIdToken idToken = (OidcIdToken) original;
                yield new OidcIdToken(
                        tokenValue,
                        idToken.getIssuedAt(),
                        idToken.getExpiresAt(),
                        idToken.getClaims()
                );
            }
            case REFRESH_TOKEN -> new OAuth2RefreshToken(
                    tokenValue,
                    original.getIssuedAt(),
                    original.getExpiresAt()
            );
            case USER_CODE -> new OAuth2UserCode(
                    tokenValue,
                    original.getIssuedAt(),
                    original.getExpiresAt()
            );
            case DEVICE_CODE -> new OAuth2DeviceCode(
                    tokenValue,
                    original.getIssuedAt(),
                    original.getExpiresAt()
            );
            case STATE -> throw new IllegalArgumentException("State is not an OAuth2 token");
        };
    }

    private <T extends OAuth2Token> String digest(
            OAuth2Authorization authorization,
            Class<T> tokenClass
    ) {
        OAuth2Authorization.Token<T> token = authorization.getToken(tokenClass);
        if (token == null) {
            return null;
        }
        return digest(token.getToken().getTokenValue(), TokenKind.from(tokenClass));
    }

    private String digest(String tokenValue, TokenKind tokenKind) {
        String placeholderPrefix =
                PLACEHOLDER_PREFIX + tokenKind.name().toLowerCase() + ":";
        if (tokenValue.startsWith(placeholderPrefix)) {
            String digest = tokenValue.substring(placeholderPrefix.length());
            if (digest.matches("[0-9a-f]{64}")) {
                return digest;
            }
            throw new IllegalStateException("Invalid stored token digest placeholder");
        }
        return tokenHasher.digest(tokenValue);
    }

    public enum TokenKind {
        STATE(null),
        AUTHORIZATION_CODE(OAuth2AuthorizationCode.class),
        ACCESS_TOKEN(OAuth2AccessToken.class),
        OIDC_ID_TOKEN(OidcIdToken.class),
        REFRESH_TOKEN(OAuth2RefreshToken.class),
        USER_CODE(OAuth2UserCode.class),
        DEVICE_CODE(OAuth2DeviceCode.class);

        private final Class<? extends OAuth2Token> tokenClass;

        TokenKind(Class<? extends OAuth2Token> tokenClass) {
            this.tokenClass = tokenClass;
        }

        private static TokenKind from(Class<? extends OAuth2Token> tokenClass) {
            for (TokenKind tokenKind : values()) {
                if (tokenClass.equals(tokenKind.tokenClass)) {
                    return tokenKind;
                }
            }
            throw new IllegalArgumentException("Unsupported token class: " + tokenClass.getName());
        }
    }
}
