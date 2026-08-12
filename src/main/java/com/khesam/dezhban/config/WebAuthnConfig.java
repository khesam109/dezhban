package com.khesam.dezhban.config;

import com.khesam.dezhban.security.JpaPublicKeyCredentialUserEntityRepository;
import com.khesam.dezhban.security.JpaUserCredentialRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

@Configuration
@EnableConfigurationProperties(WebAuthnProperties.class)
public class WebAuthnConfig {

    @Bean
    PublicKeyCredentialRpEntity relyingParty(WebAuthnProperties properties) {
        return PublicKeyCredentialRpEntity.builder()
                .name(properties.getRpName())
                .id(properties.getRpId())
                .build();
    }

    @Bean
    WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
            WebAuthnProperties properties,
            PublicKeyCredentialRpEntity relyingParty,
            JpaPublicKeyCredentialUserEntityRepository userEntityRepository,
            JpaUserCredentialRepository userCredentialRepository
    ) {
        Webauthn4JRelyingPartyOperations operations = new Webauthn4JRelyingPartyOperations(
                userEntityRepository,
                userCredentialRepository,
                relyingParty,
                properties.getAllowedOrigins()
        );
        operations.setCustomizeCreationOptions(builder -> builder
                .attestation(AttestationConveyancePreference.NONE)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                        .residentKey(ResidentKeyRequirement.DISCOURAGED)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build())
        );
        operations.setCustomizeRequestOptions(builder -> builder
                .userVerification(UserVerificationRequirement.REQUIRED)
        );
        return operations;
    }
}
