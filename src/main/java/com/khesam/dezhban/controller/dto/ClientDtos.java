package com.khesam.dezhban.controller.dto;

import com.khesam.dezhban.common.ClientAuthenticationType;
import com.khesam.dezhban.common.ClientType;
import com.khesam.dezhban.common.GrantType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Set;

public final class ClientDtos {

    private ClientDtos() {
    }

    public record Settings(
            boolean requireProofKey,
            boolean requireAuthorizationConsent
    ) {
    }

    public record ApProfileRequest(
            @NotBlank @Size(max = 255) String apTitle,
            @NotBlank @Size(max = 255) String apCode,
            @NotBlank @Size(max = 255) String apCallbackUrl
    ) {
    }

    public record CreateRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._~-]{3,100}") String clientId,
            boolean enabled,
            boolean publicClient,
            String clientSecret,
            Instant secretExpiresAt,
            @NotNull ClientType clientType,
            @NotEmpty Set<ClientAuthenticationType> authenticationMethods,
            @NotEmpty Set<GrantType> grantTypes,
            @NotNull Set<@NotBlank String> redirectUris,
            String postLogoutRedirectUri,
            @NotEmpty Set<@NotBlank String> scopes,
            @NotNull @Valid Settings clientSettings,
            @NotNull JsonNode tokenSettings,
            Instant notBefore,
            @Valid ApProfileRequest apProfile
    ) {
    }

    public record ReplaceRequest(
            boolean enabled,
            boolean publicClient,
            Instant secretExpiresAt,
            @NotNull ClientType clientType,
            @NotEmpty Set<ClientAuthenticationType> authenticationMethods,
            @NotEmpty Set<GrantType> grantTypes,
            @NotNull Set<@NotBlank String> redirectUris,
            String postLogoutRedirectUri,
            @NotEmpty Set<@NotBlank String> scopes,
            @NotNull @Valid Settings clientSettings,
            @NotNull JsonNode tokenSettings,
            Instant notBefore,
            @Valid ApProfileRequest apProfile
    ) {
    }

    public record SecretRotationRequest(Instant expiresAt) {
    }

    public record ApProfileResponse(
            String apTitle,
            String apCode,
            String apCallbackUrl
    ) {
    }

    public record Response(
            String clientId,
            boolean enabled,
            boolean publicClient,
            boolean secretConfigured,
            Instant secretExpiresAt,
            ClientType clientType,
            Set<ClientAuthenticationType> authenticationMethods,
            Set<GrantType> grantTypes,
            Set<String> redirectUris,
            String postLogoutRedirectUri,
            Set<String> scopes,
            Settings clientSettings,
            JsonNode tokenSettings,
            Instant notBefore,
            boolean locked,
            Instant lockUntil,
            int failedAuthenticationAttempts,
            Instant failedAuthenticationAt,
            Instant createdAt,
            Instant modifiedAt,
            long version,
            ApProfileResponse apProfile
    ) {
    }

    public record CreatedResponse(Response client, String clientSecret) {
    }

    public record SecretResponse(String clientSecret, Instant expiresAt) {
    }
}
