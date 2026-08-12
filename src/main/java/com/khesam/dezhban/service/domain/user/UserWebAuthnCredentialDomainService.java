package com.khesam.dezhban.service.domain.user;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserWebAuthnCredentialEntity;
import com.khesam.dezhban.dataaccess.local.repository.UserWebAuthnCredentialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserWebAuthnCredentialDomainService {

    private final UserWebAuthnCredentialRepository credentialRepository;

    public UserWebAuthnCredentialDomainService(UserWebAuthnCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    public List<UserWebAuthnCredentialEntity> listForUser(long userId) {
        return credentialRepository.findAllByEndUserIdOrderByIdAsc(userId);
    }

    public Optional<UserWebAuthnCredentialEntity> findByCredentialId(String credentialId) {
        return credentialRepository.findByCredentialId(credentialId);
    }

    public UserWebAuthnCredentialEntity register(EndUserEntity user, RegistrationData data) {
        UserWebAuthnCredentialEntity credential = new UserWebAuthnCredentialEntity();
        credential.setEndUser(user);
        credential.setCredentialId(data.credentialId());
        credential.setPublicKey(data.publicKeyCose());
        credential.setSignCount(data.signCount());
        credential.setAttestationFormat(data.attestationFormat());
        credential.setTransport(data.transports());
        credential.setDeviceBound(data.deviceBound());
        credential.setLabel(data.label());
        return credentialRepository.save(credential);
    }

    public void updateSignCount(UserWebAuthnCredentialEntity credential, long signCount) {
        credential.setSignCount(signCount);
    }

    public void delete(UserWebAuthnCredentialEntity credential) {
        credentialRepository.delete(credential);
    }

    public void deleteAllForUser(long userId) {
        credentialRepository.deleteAllByEndUserId(userId);
    }

    public record RegistrationData(
            String credentialId,
            String publicKeyCose,
            long signCount,
            String attestationFormat,
            String transports,
            boolean deviceBound,
            String label
    ) {
    }
}
