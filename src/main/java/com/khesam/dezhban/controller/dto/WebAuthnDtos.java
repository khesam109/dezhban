package com.khesam.dezhban.controller.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class WebAuthnDtos {

    private WebAuthnDtos() {
    }

    public record RegistrationOptionsRequest(
            @Size(max = 100) String label
    ) {
    }

    public record RegistrationOptionsResponse(
            String challengeId,
            JsonNode publicKey
    ) {
    }

    public record FinishRegistrationRequest(
            String challengeId,
            JsonNode credential,
            @Size(max = 100) String label
    ) {
    }

    public record AuthenticationOptionsRequest(
            String username
    ) {
    }

    public record AuthenticationOptionsResponse(
            String challengeId,
            JsonNode publicKey
    ) {
    }

    public record FinishAuthenticationRequest(
            String challengeId,
            JsonNode credential
    ) {
    }

    public record CredentialResponse(
            String credentialId,
            String label,
            boolean deviceBound,
            String transports,
            long signCount,
            Instant createdAt
    ) {
    }
}
