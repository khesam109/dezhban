package com.khesam.dezhban.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "security.webauthn")
public class WebAuthnProperties {

    /**
     * Relying Party id. Defaults to the effective hostname of the login page when blank.
     */
    private String rpId;

    /**
     * Relying Party display name.
     */
    private String rpName = "Dezhban";

    /**
     * Fully-qualified origins allowed to complete WebAuthn ceremonies.
     */
    private Set<String> allowedOrigins = Set.of("http://localhost:8585");

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }

    public String getRpName() {
        return rpName;
    }

    public void setRpName(String rpName) {
        this.rpName = rpName;
    }

    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(Set<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
