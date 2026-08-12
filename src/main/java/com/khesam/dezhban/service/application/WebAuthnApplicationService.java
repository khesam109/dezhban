package com.khesam.dezhban.service.application;

import com.khesam.dezhban.common.WebAuthnJsonParser;
import com.khesam.dezhban.common.WebAuthnJsonWriter;
import com.khesam.dezhban.controller.dto.WebAuthnDtos;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserWebAuthnCredentialEntity;
import com.khesam.dezhban.security.WebAuthnChallengeStore;
import com.khesam.dezhban.service.domain.support.DomainException;
import com.khesam.dezhban.service.domain.user.EndUserDomainService;
import com.khesam.dezhban.service.domain.user.UserWebAuthnCredentialDomainService;
import com.khesam.dezhban.service.domain.user.WebAuthnUserHandle;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public class WebAuthnApplicationService {

    private final EndUserDomainService endUserDomainService;
    private final UserWebAuthnCredentialDomainService credentialDomainService;
    private final WebAuthnRelyingPartyOperations relyingPartyOperations;
    private final WebAuthnChallengeStore challengeStore;
    private final WebAuthnJsonParser jsonParser;
    private final WebAuthnJsonWriter jsonWriter;
    private final UserDetailsService userDetailsService;
    private final org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity relyingParty;
    private final com.khesam.dezhban.security.AuthenticationAttemptService authenticationAttemptService;

    public WebAuthnApplicationService(
            EndUserDomainService endUserDomainService,
            UserWebAuthnCredentialDomainService credentialDomainService,
            WebAuthnRelyingPartyOperations relyingPartyOperations,
            WebAuthnChallengeStore challengeStore,
            WebAuthnJsonParser jsonParser,
            WebAuthnJsonWriter jsonWriter,
            UserDetailsService userDetailsService,
            org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity relyingParty,
            com.khesam.dezhban.security.AuthenticationAttemptService authenticationAttemptService
    ) {
        this.endUserDomainService = endUserDomainService;
        this.credentialDomainService = credentialDomainService;
        this.relyingPartyOperations = relyingPartyOperations;
        this.challengeStore = challengeStore;
        this.jsonParser = jsonParser;
        this.jsonWriter = jsonWriter;
        this.userDetailsService = userDetailsService;
        this.relyingParty = relyingParty;
        this.authenticationAttemptService = authenticationAttemptService;
    }

    @Transactional
    public WebAuthnDtos.RegistrationOptionsResponse startRegistration(String username, String label) {
        EndUserEntity user = endUserDomainService.requireByUsername(username);
        ensureHandle(user);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PublicKeyCredentialCreationOptions options = relyingPartyOperations.createPublicKeyCredentialCreationOptions(
                new ImmutablePublicKeyCredentialCreationOptionsRequest(authentication)
        );
        String challengeId = challengeStore.issue(username, options);
        return new WebAuthnDtos.RegistrationOptionsResponse(challengeId, jsonWriter.creationOptions(options));
    }

    @Transactional
    public WebAuthnDtos.CredentialResponse finishRegistration(
            String username,
            String challengeId,
            tools.jackson.databind.JsonNode credential,
            String label
    ) {
        EndUserEntity user = endUserDomainService.requireByUsername(username);
        WebAuthnChallengeStore.Challenge challenge = challengeStore.consume(challengeId, username);
        PublicKeyCredentialCreationOptions options = (PublicKeyCredentialCreationOptions) challenge.options();
        PublicKeyCredential<AuthenticatorAttestationResponse> publicKey =
                jsonParser.parseRegistrationCredential(credential);
        CredentialRecord record = relyingPartyOperations.registerCredential(
                new ImmutableRelyingPartyRegistrationRequest(
                        options,
                        new RelyingPartyPublicKey(publicKey, label == null || label.isBlank() ? "Passkey" : label)
                )
        );
        if (record.isBackupEligible()) {
            credentialDomainService.findByCredentialId(record.getCredentialId().toBase64UrlString())
                    .ifPresent(credentialDomainService::delete);
            throw DomainException.invalid("Only platform-bound (non-synced) authenticators are allowed");
        }
        UserWebAuthnCredentialEntity saved = credentialDomainService
                .findByCredentialId(record.getCredentialId().toBase64UrlString())
                .orElseThrow(() -> DomainException.notFound("Registered credential not found"));
        // Keep the authoritative IS_DEVICE_BOUND flag in sync with the attestation result.
        saved.setDeviceBound(!record.isBackupEligible());
        return toResponse(saved);
    }

    @Transactional
    public WebAuthnDtos.AuthenticationOptionsResponse startAuthentication(String username) {
        EndUserEntity user = endUserDomainService.requireByUsername(username);
        ensureHandle(user);
        // Build request options directly so allowCredentials contains only this user's platform credentials.
        PublicKeyCredentialRequestOptions options = PublicKeyCredentialRequestOptions.builder()
                .challenge(Bytes.random())
                .rpId(relyingParty.getId())
                .allowCredentials(credentialDomainService.listForUser(user.getId()).stream()
                        .map(WebAuthnApplicationService::toDescriptor)
                        .toList())
                .userVerification(org.springframework.security.web.webauthn.api.UserVerificationRequirement.REQUIRED)
                .timeout(java.time.Duration.ofMinutes(5))
                .build();
        String challengeId = challengeStore.issue(username, options);
        return new WebAuthnDtos.AuthenticationOptionsResponse(challengeId, jsonWriter.requestOptions(options));
    }

    @Transactional
    public Authentication finishAuthentication(
            String username,
            String challengeId,
            tools.jackson.databind.JsonNode credential,
            HttpServletRequest request
    ) {
        WebAuthnChallengeStore.Challenge challenge = challengeStore.consume(challengeId, username);
        PublicKeyCredentialRequestOptions options = (PublicKeyCredentialRequestOptions) challenge.options();
        PublicKeyCredential<AuthenticatorAssertionResponse> publicKey =
                jsonParser.parseAuthenticationCredential(credential);
        PublicKeyCredentialUserEntity userEntity = relyingPartyOperations.authenticate(
                new RelyingPartyAuthenticationRequest(options, publicKey)
        );
        if (!userEntity.getName().equals(username)) {
            throw DomainException.invalid("WebAuthn credential does not belong to the requested user");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEntity.getName());
        Authentication authentication = new WebAuthnAuthentication(
                userEntity,
                userDetails.getAuthorities()
        );
        authenticationAttemptService.recordUserSuccess(
                username,
                new com.khesam.dezhban.security.AuthenticationAttemptService.RequestDetails(
                        request.getRemoteAddr(),
                        request.getSession(false) == null ? null : request.getSession(false).getId()
                )
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        return authentication;
    }

    @Transactional(readOnly = true)
    public List<WebAuthnDtos.CredentialResponse> listCredentials(String username) {
        EndUserEntity user = endUserDomainService.requireByUsername(username);
        return credentialDomainService.listForUser(user.getId()).stream()
                .map(WebAuthnApplicationService::toResponse)
                .toList();
    }

    @Transactional
    public void deleteCredential(String username, String credentialId) {
        EndUserEntity user = endUserDomainService.requireByUsername(username);
        UserWebAuthnCredentialEntity credential = credentialDomainService.findByCredentialId(credentialId)
                .orElseThrow(() -> DomainException.notFound("Credential not found"));
        if (credential.getEndUser().getId() != user.getId()) {
            throw DomainException.notFound("Credential not found");
        }
        credentialDomainService.delete(credential);
    }

    private void ensureHandle(EndUserEntity user) {
        if (user.getWebAuthnHandle() == null || user.getWebAuthnHandle().isBlank()) {
            user.setWebAuthnHandle(WebAuthnUserHandle.generate());
        }
    }

    private static org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor toDescriptor(UserWebAuthnCredentialEntity credential) {
        return org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor.builder()
                .type(org.springframework.security.web.webauthn.api.PublicKeyCredentialType.PUBLIC_KEY)
                .id(Bytes.fromBase64(credential.getCredentialId()))
                .transports(credential.getTransports() == null || credential.getTransports().isBlank()
                        ? java.util.Set.of()
                        : java.util.Arrays.stream(credential.getTransports().split(","))
                                .map(String::trim)
                                .filter(value -> !value.isBlank())
                                .map(org.springframework.security.web.webauthn.api.AuthenticatorTransport::valueOf)
                                .collect(java.util.stream.Collectors.toSet()))
                .build();
    }

    private static WebAuthnDtos.CredentialResponse toResponse(UserWebAuthnCredentialEntity credential) {
        return new WebAuthnDtos.CredentialResponse(
                credential.getCredentialId(),
                credential.getLabel(),
                credential.isDeviceBound(),
                credential.getTransports(),
                credential.getSignCount(),
                credential.getCreatedAt()
        );
    }
}
