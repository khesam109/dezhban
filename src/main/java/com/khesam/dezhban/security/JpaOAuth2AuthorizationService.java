package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.OAuth2AuthorizationEntity;
import com.khesam.dezhban.dataaccess.local.repository.OAuth2AuthorizationRepository;
import com.khesam.dezhban.security.OAuth2AuthorizationMapper.TokenKind;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final OAuth2AuthorizationRepository authorizationRepository;
    private final OAuth2AuthorizationMapper authorizationMapper;
    private final Sha256TokenHasher tokenHasher;

    public JpaOAuth2AuthorizationService(
            OAuth2AuthorizationRepository authorizationRepository,
            OAuth2AuthorizationMapper authorizationMapper,
            Sha256TokenHasher tokenHasher
    ) {
        this.authorizationRepository = authorizationRepository;
        this.authorizationMapper = authorizationMapper;
        this.tokenHasher = tokenHasher;
    }

    @Override
    @Transactional
    public void save(OAuth2Authorization authorization) {
        OAuth2AuthorizationEntity entity = authorizationRepository
                .findById(authorization.getId())
                .orElseGet(OAuth2AuthorizationEntity::new);
        authorizationMapper.updateEntity(authorization, entity);
        authorizationRepository.save(entity);
    }

    @Override
    @Transactional
    public void remove(OAuth2Authorization authorization) {
        authorizationRepository.deleteById(authorization.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable OAuth2Authorization findById(String id) {
        return authorizationRepository.findById(id)
                .map(entity -> authorizationMapper.toAuthorization(entity, null, null))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable OAuth2Authorization findByToken(
            String token,
            @Nullable OAuth2TokenType tokenType
    ) {
        String digest = tokenHasher.digest(token);
        if (tokenType == null) {
            return findByAnyTokenDigest(digest, token);
        }

        TokenKind tokenKind = toTokenKind(tokenType);
        if (tokenKind == null) {
            return null;
        }
        return findEntity(digest, tokenKind)
                .map(entity -> authorizationMapper.toAuthorization(entity, tokenKind, token))
                .orElse(null);
    }

    private OAuth2Authorization findByAnyTokenDigest(String digest, String presentedToken) {
        List<OAuth2AuthorizationEntity> matches =
                authorizationRepository.findAllByTokenDigest(digest);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Token digest matched multiple authorizations");
        }

        OAuth2AuthorizationEntity entity = matches.getFirst();
        TokenKind tokenKind = matchingTokenKind(entity, digest);
        return authorizationMapper.toAuthorization(entity, tokenKind, presentedToken);
    }

    private Optional<OAuth2AuthorizationEntity> findEntity(
            String digest,
            TokenKind tokenKind
    ) {
        return switch (tokenKind) {
            case STATE -> authorizationRepository.findByStateDigest(digest);
            case AUTHORIZATION_CODE ->
                    authorizationRepository.findByAuthorizationCodeDigest(digest);
            case ACCESS_TOKEN -> authorizationRepository.findByAccessTokenDigest(digest);
            case OIDC_ID_TOKEN -> authorizationRepository.findByOidcIdTokenDigest(digest);
            case REFRESH_TOKEN -> authorizationRepository.findByRefreshTokenDigest(digest);
            case USER_CODE -> authorizationRepository.findByUserCodeDigest(digest);
            case DEVICE_CODE -> authorizationRepository.findByDeviceCodeDigest(digest);
        };
    }

    private TokenKind matchingTokenKind(
            OAuth2AuthorizationEntity entity,
            String digest
    ) {
        if (digest.equals(entity.getStateDigest())) {
            return TokenKind.STATE;
        }
        if (digest.equals(entity.getAuthorizationCodeDigest())) {
            return TokenKind.AUTHORIZATION_CODE;
        }
        if (digest.equals(entity.getAccessTokenDigest())) {
            return TokenKind.ACCESS_TOKEN;
        }
        if (digest.equals(entity.getOidcIdTokenDigest())) {
            return TokenKind.OIDC_ID_TOKEN;
        }
        if (digest.equals(entity.getRefreshTokenDigest())) {
            return TokenKind.REFRESH_TOKEN;
        }
        if (digest.equals(entity.getUserCodeDigest())) {
            return TokenKind.USER_CODE;
        }
        if (digest.equals(entity.getDeviceCodeDigest())) {
            return TokenKind.DEVICE_CODE;
        }
        throw new IllegalStateException("Authorization did not contain matched token digest");
    }

    private TokenKind toTokenKind(OAuth2TokenType tokenType) {
        return switch (tokenType.getValue()) {
            case OAuth2ParameterNames.STATE -> TokenKind.STATE;
            case OAuth2ParameterNames.CODE -> TokenKind.AUTHORIZATION_CODE;
            case OAuth2ParameterNames.ACCESS_TOKEN -> TokenKind.ACCESS_TOKEN;
            case OidcParameterNames.ID_TOKEN -> TokenKind.OIDC_ID_TOKEN;
            case OAuth2ParameterNames.REFRESH_TOKEN -> TokenKind.REFRESH_TOKEN;
            case OAuth2ParameterNames.USER_CODE -> TokenKind.USER_CODE;
            case OAuth2ParameterNames.DEVICE_CODE -> TokenKind.DEVICE_CODE;
            default -> null;
        };
    }
}
