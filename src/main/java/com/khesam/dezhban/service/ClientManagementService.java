package com.khesam.dezhban.service;

import com.khesam.dezhban.common.ClientAuthenticationType;
import com.khesam.dezhban.common.ClientType;
import com.khesam.dezhban.common.GrantType;
import com.khesam.dezhban.controller.dto.ClientDtos;
import com.khesam.dezhban.controller.dto.PageResponse;
import com.khesam.dezhban.controller.error.ApiException;
import com.khesam.dezhban.dataaccess.local.entity.ApClientProfileEntity;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Service
public class ClientManagementService {

    private final ClientRepository clientRepository;
    private final ApClientProfileRepository apClientProfileRepository;
    private final OAuth2AuthorizationRepository authorizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientManagementService(
            ClientRepository clientRepository,
            ApClientProfileRepository apClientProfileRepository,
            OAuth2AuthorizationRepository authorizationRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            EntityManager entityManager
    ) {
        this.clientRepository = clientRepository;
        this.apClientProfileRepository = apClientProfileRepository;
        this.authorizationRepository = authorizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientDtos.Response> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.from(clientRepository
                .findAll(PageRequest.of(Math.max(page, 0), safeSize, Sort.by("clientId")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ClientDtos.Response get(String clientId) {
        return toResponse(find(clientId));
    }

    @Transactional
    public ClientDtos.CreatedResponse create(ClientDtos.CreateRequest request) {
        if (clientRepository.existsByClientId(request.clientId())) {
            throw conflict("Client ID already exists");
        }
        validateConfiguration(
                request.publicClient(),
                request.authenticationMethods(),
                request.grantTypes(),
                request.redirectUris(),
                request.clientSettings(),
                request.clientType(),
                request.apProfile()
        );

        String rawSecret = null;
        if (!request.publicClient()) {
            rawSecret = request.clientSecret() == null || request.clientSecret().isBlank()
                    ? generateSecret()
                    : request.clientSecret();
        }

        ClientEntity client = new ClientEntity();
        client.setClientId(request.clientId());
        client.setEnabled(request.enabled());
        client.setPublicClient(request.publicClient());
        client.setSecretHash(rawSecret == null ? null : passwordEncoder.encode(rawSecret));
        client.setSecretExpiresAt(request.secretExpiresAt());
        applyConfiguration(
                client,
                request.clientType(),
                request.authenticationMethods(),
                request.grantTypes(),
                request.redirectUris(),
                request.postLogoutRedirectUri(),
                request.scopes(),
                request.clientSettings(),
                request.tokenSettings(),
                request.notBefore()
        );
        clientRepository.saveAndFlush(client);
        replaceApProfile(client, request.apProfile());
        entityManager.flush();
        return new ClientDtos.CreatedResponse(toResponse(client), rawSecret);
    }

    @Transactional
    public ClientDtos.Response replace(
            String clientId,
            String ifMatch,
            ClientDtos.ReplaceRequest request
    ) {
        ClientEntity client = find(clientId);
        requireMatchingVersion(ifMatch, client.getVersion());
        validateConfiguration(
                request.publicClient(),
                request.authenticationMethods(),
                request.grantTypes(),
                request.redirectUris(),
                request.clientSettings(),
                request.clientType(),
                request.apProfile()
        );
        if (!request.publicClient() && client.getSecretHash() == null) {
            throw invalid("A confidential client requires a client secret");
        }
        client.setEnabled(request.enabled());
        client.setPublicClient(request.publicClient());
        if (request.publicClient()) {
            client.setSecretHash(null);
            client.setSecretExpiresAt(null);
        } else {
            client.setSecretExpiresAt(request.secretExpiresAt());
        }
        applyConfiguration(
                client,
                request.clientType(),
                request.authenticationMethods(),
                request.grantTypes(),
                request.redirectUris(),
                request.postLogoutRedirectUri(),
                request.scopes(),
                request.clientSettings(),
                request.tokenSettings(),
                request.notBefore()
        );
        replaceApProfile(client, request.apProfile());
        entityManager.flush();
        return toResponse(client);
    }

    @Transactional
    public ClientDtos.Response patch(String clientId, String ifMatch, JsonNode patch) {
        if (!patch.isObject()) {
            throw invalid("JSON merge patch must be an object");
        }
        ClientEntity client = find(clientId);
        requireMatchingVersion(ifMatch, client.getVersion());
        for (var field : patch.properties()) {
            switch (field.getKey()) {
                case "enabled" -> client.setEnabled(requiredBoolean(field.getValue(), "enabled"));
                case "notBefore" -> client.setNotBefore(
                        field.getValue().isNull()
                                ? null
                                : Instant.parse(requiredText(field.getValue(), "notBefore"))
                );
                case "secretExpiresAt" -> client.setSecretExpiresAt(
                        field.getValue().isNull()
                                ? null
                                : Instant.parse(requiredText(field.getValue(), "secretExpiresAt"))
                );
                default -> throw invalid("Unsupported client patch field: " + field.getKey());
            }
        }
        entityManager.flush();
        return toResponse(client);
    }

    @Transactional
    public ClientDtos.SecretResponse rotateSecret(
            String clientId,
            String ifMatch,
            ClientDtos.SecretRotationRequest request
    ) {
        ClientEntity client = find(clientId);
        requireMatchingVersion(ifMatch, client.getVersion());
        if (client.isPublicClient()) {
            throw invalid("Public clients do not have shared secrets");
        }
        String rawSecret = generateSecret();
        client.setSecretHash(passwordEncoder.encode(rawSecret));
        client.setSecretExpiresAt(request.expiresAt());
        authorizationRepository.deleteAllByRegisteredClientId(Long.toString(client.getId()));
        entityManager.flush();
        return new ClientDtos.SecretResponse(rawSecret, request.expiresAt());
    }

    @Transactional
    public void delete(String clientId, String ifMatch) {
        ClientEntity client = find(clientId);
        requireMatchingVersion(ifMatch, client.getVersion());
        authorizationRepository.deleteAllByRegisteredClientId(Long.toString(client.getId()));
        clientRepository.delete(client);
    }

    private ClientEntity find(String clientId) {
        return clientRepository.findByClientId(clientId)
                .orElseThrow(() -> notFound("Client not found"));
    }

    private void applyConfiguration(
            ClientEntity client,
            ClientType clientType,
            Set<ClientAuthenticationType> authenticationMethods,
            Set<GrantType> grantTypes,
            Set<String> redirectUris,
            String postLogoutRedirectUri,
            Set<String> scopes,
            ClientDtos.Settings settings,
            JsonNode tokenSettings,
            Instant notBefore
    ) {
        client.setClientType(clientType);
        client.setAuthenticationMethods(Set.copyOf(authenticationMethods));
        client.setGrantTypes(Set.copyOf(grantTypes));
        client.setRedirectUris(Set.copyOf(redirectUris));
        client.setPostLogoutRedirectUri(postLogoutRedirectUri);
        client.setScopes(Set.copyOf(scopes));
        client.setClientSettings(
                "{\"requireProofKey\":" + settings.requireProofKey()
                        + ",\"requireAuthorizationConsent\":"
                        + settings.requireAuthorizationConsent() + "}"
        );
        client.setTokenSettings(tokenSettings.toString());
        client.setNotBefore(notBefore);
    }

    private void validateConfiguration(
            boolean publicClient,
            Set<ClientAuthenticationType> authenticationMethods,
            Set<GrantType> grantTypes,
            Set<String> redirectUris,
            ClientDtos.Settings settings,
            ClientType clientType,
            ClientDtos.ApProfileRequest apProfile
    ) {
        if (publicClient) {
            if (!authenticationMethods.equals(Set.of(ClientAuthenticationType.NONE))) {
                throw invalid("Public clients must use only the NONE authentication method");
            }
            if (!settings.requireProofKey()) {
                throw invalid("Public authorization-code clients must require PKCE");
            }
            if (grantTypes.contains(GrantType.CLIENT_CREDENTIALS)) {
                throw invalid("Public clients cannot use the client_credentials grant");
            }
        } else if (authenticationMethods.contains(ClientAuthenticationType.NONE)) {
            throw invalid("Confidential clients cannot use the NONE authentication method");
        }
        if (grantTypes.contains(GrantType.AUTHORIZATION_CODE) && redirectUris.isEmpty()) {
            throw invalid("Authorization-code clients require a redirect URI");
        }
        if (clientType == ClientType.AP && apProfile == null) {
            throw invalid("AP clients require an AP profile");
        }
        if (clientType != ClientType.AP && apProfile != null) {
            throw invalid("Only AP clients may have an AP profile");
        }
        if (redirectUris.stream().anyMatch(uri -> uri.contains("#"))) {
            throw invalid("Redirect URIs must not contain fragments");
        }
        if (authenticationMethods.toString().length() > 255
                || grantTypes.toString().length() > 255
                || String.join(",", redirectUris).length() > 1000) {
            throw invalid("Serialized client configuration exceeds database limits");
        }
    }

    private void replaceApProfile(ClientEntity client, ClientDtos.ApProfileRequest request) {
        if (request == null) {
            apClientProfileRepository.deleteById(client.getId());
            return;
        }
        ApClientProfileEntity profile = apClientProfileRepository.findById(client.getId())
                .orElseGet(() -> {
                    ApClientProfileEntity value = new ApClientProfileEntity();
                    value.setClient(client);
                    return value;
                });
        profile.setApTitle(request.apTitle());
        profile.setApCode(request.apCode());
        profile.setApCallbackUrl(request.apCallbackUrl());
        apClientProfileRepository.save(profile);
    }

    private ClientDtos.Response toResponse(ClientEntity client) {
        ApClientProfileEntity profile = apClientProfileRepository.findById(client.getId())
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

    private String generateSecret() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public String etag(ClientDtos.Response response) {
        return etag(response.version());
    }

    private void requireMatchingVersion(String ifMatch, long version) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ApiException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "If-Match is required"
            );
        }
        if (!etag(version).equals(ifMatch)) {
            throw new ApiException(
                    HttpStatus.PRECONDITION_FAILED,
                    "PRECONDITION_FAILED",
                    "Resource version does not match"
            );
        }
    }

    private String etag(long version) {
        return "\"client-" + version + "\"";
    }

    private String requiredText(JsonNode value, String field) {
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw invalid(field + " must be a non-empty string");
        }
        return value.asString();
    }

    private boolean requiredBoolean(JsonNode value, String field) {
        if (value == null || !value.isBoolean()) {
            throw invalid(field + " must be a boolean");
        }
        return value.asBoolean();
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RESOURCE", message);
    }
}
