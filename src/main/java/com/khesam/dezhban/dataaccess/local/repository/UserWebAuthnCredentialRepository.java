package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.UserWebAuthnCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWebAuthnCredentialRepository extends JpaRepository<UserWebAuthnCredentialEntity, Long> {

    List<UserWebAuthnCredentialEntity> findAllByEndUserIdOrderByIdAsc(long userId);

    Optional<UserWebAuthnCredentialEntity> findByCredentialId(String credentialId);

    void deleteAllByEndUserId(long userId);
}
