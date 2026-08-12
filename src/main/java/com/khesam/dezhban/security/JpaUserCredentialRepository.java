package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserWebAuthnCredentialEntity;
import com.khesam.dezhban.dataaccess.local.repository.EndUserRepository;
import com.khesam.dezhban.service.domain.user.UserWebAuthnCredentialDomainService;
import com.khesam.dezhban.service.domain.user.WebAuthnUserHandle;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JpaUserCredentialRepository implements UserCredentialRepository {

    private final UserWebAuthnCredentialDomainService credentialDomainService;
    private final EndUserRepository endUserRepository;

    public JpaUserCredentialRepository(
            UserWebAuthnCredentialDomainService credentialDomainService,
            EndUserRepository endUserRepository
    ) {
        this.credentialDomainService = credentialDomainService;
        this.endUserRepository = endUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        return credentialDomainService.findByCredentialId(credentialId.toBase64UrlString())
                .map(JpaUserCredentialRepository::toRecord)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialRecord> findByUserId(Bytes userId) {
        String handle = WebAuthnUserHandle.fromBytes(userId.getBytes());
        return endUserRepository.findAll().stream()
                .filter(user -> handle.equals(user.getWebAuthnHandle()))
                .findFirst()
                .map(user -> credentialDomainService.listForUser(user.getId()).stream()
                        .map(JpaUserCredentialRepository::toRecord)
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional
    public void save(CredentialRecord record) {
        String credentialId = record.getCredentialId().toBase64UrlString();
        UserWebAuthnCredentialEntity existing = credentialDomainService.findByCredentialId(credentialId).orElse(null);
        if (existing != null) {
            existing.setSignCount(record.getSignatureCount());
            return;
        }
        String handle = WebAuthnUserHandle.fromBytes(record.getUserEntityUserId().getBytes());
        EndUserEntity user = endUserRepository.findAll().stream()
                .filter(candidate -> handle.equals(candidate.getWebAuthnHandle()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("WebAuthn user handle does not map to a user"));
        credentialDomainService.register(
                user,
                new UserWebAuthnCredentialDomainService.RegistrationData(
                        credentialId,
                        record.getPublicKey().getBytes() == null ? "" : java.util.Base64.getEncoder().encodeToString(record.getPublicKey().getBytes()),
                        record.getSignatureCount(),
                        record.getAttestationObject() == null ? "" : record.getAttestationObject().toBase64UrlString(),
                        record.getTransports() == null ? null : record.getTransports().stream().map(AuthenticatorTransport::getValue).reduce((a, b) -> a + "," + b).orElse(null),
                        !record.isBackupEligible(),
                        record.getLabel()
                )
        );
    }

    @Override
    @Transactional
    public void delete(Bytes credentialId) {
        credentialDomainService.findByCredentialId(credentialId.toBase64UrlString())
                .ifPresent(credentialDomainService::delete);
    }

    private static CredentialRecord toRecord(UserWebAuthnCredentialEntity entity) {
        var builder = ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                .credentialId(Bytes.fromBase64(entity.getCredentialId()))
                .userEntityUserId(new Bytes(WebAuthnUserHandle.toBytes(entity.getEndUser().getWebAuthnHandle())))
                .publicKey(new ImmutablePublicKeyCose(java.util.Base64.getDecoder().decode(entity.getPublicKey())))
                .signatureCount(entity.getSignCount())
                .uvInitialized(false)
                .backupEligible(!entity.isDeviceBound())
                .backupState(false)
                .attestationObject(entity.getAttestationFormat() == null || entity.getAttestationFormat().isBlank() ? null : Bytes.fromBase64(entity.getAttestationFormat()))
                .attestationClientDataJSON(new Bytes(new byte[0]))
                .created(entity.getCreatedAt())
                .lastUsed(entity.getCreatedAt())
                .label(entity.getLabel());
        if (entity.getTransports() != null && !entity.getTransports().isBlank()) {
            var transports = java.util.Arrays.stream(entity.getTransports().split(","))
                    .filter(value -> !value.isBlank())
                    .map(AuthenticatorTransport::valueOf)
                    .collect(java.util.stream.Collectors.toSet());
            builder.transports(transports);
        }
        return builder.build();
    }
}
