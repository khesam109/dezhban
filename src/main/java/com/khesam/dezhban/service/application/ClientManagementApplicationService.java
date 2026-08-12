package com.khesam.dezhban.service.application;

import com.khesam.dezhban.controller.dto.ClientDtos;
import com.khesam.dezhban.controller.dto.PageResponse;
import com.khesam.dezhban.dataaccess.local.entity.ApClientProfileEntity;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.service.domain.authorization.OAuth2AuthorizationDomainService;
import com.khesam.dezhban.service.domain.client.ApClientProfileDomainService;
import com.khesam.dezhban.service.domain.client.ClientDomainService;
import com.khesam.dezhban.service.domain.support.DomainException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
public class ClientManagementApplicationService {

    private final ClientDomainService clientDomainService;
    private final ApClientProfileDomainService apClientProfileDomainService;
    private final OAuth2AuthorizationDomainService authorizationDomainService;
    private final ObjectMapper objectMapper;

    public ClientManagementApplicationService(
            ClientDomainService clientDomainService,
            ApClientProfileDomainService apClientProfileDomainService,
            OAuth2AuthorizationDomainService authorizationDomainService,
            ObjectMapper objectMapper
    ) {
        this.clientDomainService = clientDomainService;
        this.apClientProfileDomainService = apClientProfileDomainService;
        this.authorizationDomainService = authorizationDomainService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientDtos.Response> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.from(clientDomainService
                .list(PageRequest.of(Math.max(page, 0), safeSize, Sort.by("clientId")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ClientDtos.Response get(String clientId) {
        return toResponse(clientDomainService.requireByClientId(clientId));
    }

    @Transactional
    public ClientDtos.CreatedResponse create(ClientDtos.CreateRequest request) {
        ClientDomainService.CreatedClient created = clientDomainService.create(
                new ClientDomainService.CreateCommand(
                        request.clientId(),
                        request.enabled(),
                        request.publicClient(),
                        request.clientSecret(),
                        request.secretExpiresAt(),
                        toConfiguration(
                                request.publicClient(),
                                request.clientType(),
                                request.authenticationMethods(),
                                request.grantTypes(),
                                request.redirectUris(),
                                request.postLogoutRedirectUri(),
                                request.scopes(),
                                request.clientSettings(),
                                request.tokenSettings(),
                                request.notBefore(),
                                request.apProfile() != null
                        )
                )
        );
        apClientProfileDomainService.replace(
                created.client(),
                toApProfileData(request.apProfile())
        );
        clientDomainService.flush();
        return new ClientDtos.CreatedResponse(toResponse(created.client()), created.rawSecret());
    }

    @Transactional
    public ClientDtos.Response replace(
            String clientId,
            String ifMatch,
            ClientDtos.ReplaceRequest request
    ) {
        ClientEntity client = clientDomainService.requireByClientId(clientId);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(client.getVersion()));
        clientDomainService.replace(
                client,
                new ClientDomainService.ReplaceCommand(
                        request.enabled(),
                        request.publicClient(),
                        request.secretExpiresAt(),
                        toConfiguration(
                                request.publicClient(),
                                request.clientType(),
                                request.authenticationMethods(),
                                request.grantTypes(),
                                request.redirectUris(),
                                request.postLogoutRedirectUri(),
                                request.scopes(),
                                request.clientSettings(),
                                request.tokenSettings(),
                                request.notBefore(),
                                request.apProfile() != null
                        )
                )
        );
        apClientProfileDomainService.replace(client, toApProfileData(request.apProfile()));
        clientDomainService.flush();
        return toResponse(client);
    }

    @Transactional
    public ClientDtos.Response patch(String clientId, String ifMatch, JsonNode patch) {
        if (!patch.isObject()) {
            throw DomainException.invalid("JSON merge patch must be an object");
        }
        ClientEntity client = clientDomainService.requireByClientId(clientId);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(client.getVersion()));
        for (var field : patch.properties()) {
            switch (field.getKey()) {
                case "enabled" -> clientDomainService.patchEnabled(
                        client,
                        requiredBoolean(field.getValue(), "enabled")
                );
                case "notBefore" -> clientDomainService.patchNotBefore(
                        client,
                        field.getValue().isNull()
                                ? null
                                : Instant.parse(requiredText(field.getValue(), "notBefore"))
                );
                case "secretExpiresAt" -> clientDomainService.patchSecretExpiresAt(
                        client,
                        field.getValue().isNull()
                                ? null
                                : Instant.parse(requiredText(field.getValue(), "secretExpiresAt"))
                );
                default -> throw DomainException.invalid("Unsupported client patch field: " + field.getKey());
            }
        }
        clientDomainService.flush();
        return toResponse(client);
    }

    @Transactional
    public ClientDtos.SecretResponse rotateSecret(
            String clientId,
            String ifMatch,
            ClientDtos.SecretRotationRequest request
    ) {
        ClientEntity client = clientDomainService.requireByClientId(clientId);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(client.getVersion()));
        String rawSecret = clientDomainService.rotateSecret(client, request.expiresAt());
        authorizationDomainService.revokeByRegisteredClientId(Long.toString(client.getId()));
        clientDomainService.flush();
        return new ClientDtos.SecretResponse(rawSecret, request.expiresAt());
    }

    @Transactional
    public void delete(String clientId, String ifMatch) {
        ClientEntity client = clientDomainService.requireByClientId(clientId);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(client.getVersion()));
        authorizationDomainService.revokeByRegisteredClientId(Long.toString(client.getId()));
        clientDomainService.delete(client);
    }

    public String etag(ClientDtos.Response response) {
        return etag(response.version());
    }

    private String etag(long version) {
        return ResourceVersionGuard.etag("client", version);
    }

    private ClientDomainService.ClientConfiguration toConfiguration(
            boolean publicClient,
            com.khesam.dezhban.common.ClientType clientType,
            java.util.Set<com.khesam.dezhban.common.ClientAuthenticationType> authenticationMethods,
            java.util.Set<com.khesam.dezhban.common.GrantType> grantTypes,
            java.util.Set<String> redirectUris,
            String postLogoutRedirectUri,
            java.util.Set<String> scopes,
            ClientDtos.Settings settings,
            JsonNode tokenSettings,
            Instant notBefore,
            boolean apProfilePresent
    ) {
        return new ClientDomainService.ClientConfiguration(
                publicClient,
                clientType,
                authenticationMethods,
                grantTypes,
                redirectUris,
                postLogoutRedirectUri,
                scopes,
                settings.requireProofKey(),
                settings.requireAuthorizationConsent(),
                tokenSettings.toString(),
                notBefore,
                apProfilePresent
        );
    }

    private ApClientProfileDomainService.ProfileData toApProfileData(ClientDtos.ApProfileRequest request) {
        if (request == null) {
            return null;
        }
        return new ApClientProfileDomainService.ProfileData(
                request.apTitle(),
                request.apCode(),
                request.apCallbackUrl()
        );
    }

    private ClientDtos.Response toResponse(ClientEntity client) {
        ApClientProfileEntity profile = apClientProfileDomainService.findByClientId(client.getId())
                .orElse(null);
        JsonNode settings = readJson(client.getClientSettings());
        return new ClientDtos.Response(
                client.getClientId(),
                client.isEnabled(),
                client.isPublicClient(),
                client.getSecretHash() != null,
                client.getSecretExpiresAt(),
                client.getClientType(),
                client.getAuthenticationMethods(),
                client.getGrantTypes(),
                client.getRedirectUris(),
                client.getPostLogoutRedirectUri(),
                client.getScopes(),
                new ClientDtos.Settings(
                        settings.path("requireProofKey").asBoolean(false),
                        settings.path("requireAuthorizationConsent").asBoolean(false)
                ),
                readJson(client.getTokenSettings()),
                client.getNotBefore(),
                client.isLocked(),
                client.getLockUntil(),
                client.getFailedAuthenticationAttempts(),
                client.getFailedAuthenticationAt(),
                client.getCreatedAt(),
                client.getModifiedAt(),
                client.getVersion(),
                profile == null ? null : new ClientDtos.ApProfileResponse(
                        profile.getApTitle(),
                        profile.getApCode(),
                        profile.getApCallbackUrl()
                )
        );
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored client JSON is invalid", exception);
        }
    }

    private String requiredText(JsonNode value, String field) {
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw DomainException.invalid(field + " must be a non-empty string");
        }
        return value.asString();
    }

    private boolean requiredBoolean(JsonNode value, String field) {
        if (value == null || !value.isBoolean()) {
            throw DomainException.invalid(field + " must be a boolean");
        }
        return value.asBoolean();
    }
}
