package com.khesam.dezhban.common;

import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * JSON projection for WebAuthn options. The project runs Jackson 3 while Spring
 * Security's WebAuthn mixins target Jackson 2; this small writer keeps the JSON
 * contract explicit and avoids a parallel Jackson 2 dependency.
 */
@Component
public class WebAuthnJsonWriter {

    public JsonNode creationOptions(PublicKeyCredentialCreationOptions options) {
        var root = JsonNodeFactory.instance.objectNode();
        var rp = root.putObject("rp");
        rp.put("name", options.getRp().getName());
        if (options.getRp().getId() != null) {
            rp.put("id", options.getRp().getId());
        }
        var user = root.putObject("user");
        user.put("name", options.getUser().getName());
        user.put("displayName", options.getUser().getDisplayName());
        user.put("id", options.getUser().getId().toBase64UrlString());
        root.put("challenge", options.getChallenge().toBase64UrlString());
        root.put("timeout", options.getTimeout().toMillis());
        var params = root.putArray("pubKeyCredParams");
        options.getPubKeyCredParams().forEach(param -> {
            var node = params.addObject();
            node.put("type", param.getType().getValue());
            node.put("alg", param.getAlg().getValue());
        });
        var selection = root.putObject("authenticatorSelection");
        selection.put("authenticatorAttachment",
                options.getAuthenticatorSelection().getAuthenticatorAttachment().getValue());
        selection.put("residentKey", options.getAuthenticatorSelection().getResidentKey().getValue());
        selection.put("requireResidentKey",
                options.getAuthenticatorSelection().getResidentKey() == org.springframework.security.web.webauthn.api.ResidentKeyRequirement.REQUIRED);
        selection.put("userVerification", options.getAuthenticatorSelection().getUserVerification().getValue());
        root.put("attestation", options.getAttestation().getValue());
        var exclude = root.putArray("excludeCredentials");
        for (PublicKeyCredentialDescriptor descriptor : options.getExcludeCredentials()) {
            exclude.add(descriptor(descriptor));
        }
        return root;
    }

    public JsonNode requestOptions(PublicKeyCredentialRequestOptions options) {
        var root = JsonNodeFactory.instance.objectNode();
        root.put("challenge", options.getChallenge().toBase64UrlString());
        root.put("timeout", options.getTimeout().toMillis());
        root.put("rpId", options.getRpId());
        var allow = root.putArray("allowCredentials");
        for (PublicKeyCredentialDescriptor descriptor : options.getAllowCredentials()) {
            allow.add(descriptor(descriptor));
        }
        root.put("userVerification", options.getUserVerification().getValue());
        return root;
    }

    private tools.jackson.databind.node.ObjectNode descriptor(PublicKeyCredentialDescriptor descriptor) {
        var node = JsonNodeFactory.instance.objectNode();
        node.put("type", descriptor.getType().getValue());
        node.put("id", descriptor.getId().toBase64UrlString());
        if (descriptor.getTransports() != null && !descriptor.getTransports().isEmpty()) {
            var transports = node.putArray("transports");
            for (AuthenticatorTransport transport : descriptor.getTransports()) {
                transports.add(transport.getValue());
            }
        }
        return node;
    }
}

