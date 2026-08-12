package com.khesam.dezhban.common;

import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Component
public class WebAuthnJsonParser {

    public PublicKeyCredential<AuthenticatorAttestationResponse> parseRegistrationCredential(JsonNode node) {
        JsonNode response = node.path("response");
        return PublicKeyCredential.<AuthenticatorAttestationResponse>builder()
                .id(requiredText(node.path("id"), "id"))
                .rawId(Bytes.fromBase64(requiredText(node.path("rawId"), "rawId")))
                .type(PublicKeyCredentialType.PUBLIC_KEY)
                .response(AuthenticatorAttestationResponse.builder()
                        .clientDataJSON(Bytes.fromBase64(requiredText(response.path("clientDataJSON"), "response.clientDataJSON")))
                        .attestationObject(Bytes.fromBase64(requiredText(response.path("attestationObject"), "response.attestationObject")))
                        .transports(parseTransports(response.path("transports")))
                        .build())
                .build();
    }

    public PublicKeyCredential<AuthenticatorAssertionResponse> parseAuthenticationCredential(JsonNode node) {
        JsonNode response = node.path("response");
        var builder = AuthenticatorAssertionResponse.builder()
                .clientDataJSON(Bytes.fromBase64(requiredText(response.path("clientDataJSON"), "response.clientDataJSON")))
                .authenticatorData(Bytes.fromBase64(requiredText(response.path("authenticatorData"), "response.authenticatorData")))
                .signature(Bytes.fromBase64(requiredText(response.path("signature"), "response.signature")));
        if (!response.path("userHandle").isMissingNode() && !response.path("userHandle").isNull()) {
            builder.userHandle(Bytes.fromBase64(response.path("userHandle").asString()));
        }
        return PublicKeyCredential.<AuthenticatorAssertionResponse>builder()
                .id(requiredText(node.path("id"), "id"))
                .rawId(Bytes.fromBase64(requiredText(node.path("rawId"), "rawId")))
                .type(PublicKeyCredentialType.PUBLIC_KEY)
                .response(builder.build())
                .build();
    }

    private List<AuthenticatorTransport> parseTransports(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        var transports = new java.util.ArrayList<AuthenticatorTransport>();
        for (JsonNode value : node) {
            transports.add(AuthenticatorTransport.valueOf(value.asString()));
        }
        return transports;
    }

    private String requiredText(JsonNode node, String field) {
        if (node == null || !node.isString() || node.asString().isBlank()) {
            throw com.khesam.dezhban.service.domain.support.DomainException.invalid(
                    "WebAuthn credential field is missing or invalid: " + field
            );
        }
        return node.asString();
    }
}
