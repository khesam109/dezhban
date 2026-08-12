package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.repository.EndUserRepository;
import com.khesam.dezhban.service.domain.user.WebAuthnUserHandle;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private final EndUserRepository endUserRepository;

    public JpaPublicKeyCredentialUserEntityRepository(EndUserRepository endUserRepository) {
        this.endUserRepository = endUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        String handle = WebAuthnUserHandle.fromBytes(id.getBytes());
        return endUserRepository.findAll().stream()
                .filter(user -> handle.equals(user.getWebAuthnHandle()))
                .findFirst()
                .map(JpaPublicKeyCredentialUserEntityRepository::toUserEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        return endUserRepository.findByUsername(username)
                .map(JpaPublicKeyCredentialUserEntityRepository::toUserEntity)
                .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        // Users are managed through the admin/user management flow; WebAuthn user entities are derived.
    }

    @Override
    public void delete(Bytes id) {
        // Deletion of users is managed through the admin flow.
    }

    private static PublicKeyCredentialUserEntity toUserEntity(EndUserEntity user) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .name(user.getUsername())
                .id(new Bytes(WebAuthnUserHandle.toBytes(user.getWebAuthnHandle())))
                .displayName(user.getUsername())
                .build();
    }
}
