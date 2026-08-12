package com.khesam.dezhban.service.domain.user;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.repository.EndUserRepository;
import com.khesam.dezhban.service.domain.support.DomainException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class EndUserDomainService {

    private final EndUserRepository endUserRepository;
    private final EntityManager entityManager;

    public EndUserDomainService(EndUserRepository endUserRepository, EntityManager entityManager) {
        this.endUserRepository = endUserRepository;
        this.entityManager = entityManager;
    }

    public Page<EndUserEntity> list(Pageable pageable) {
        return endUserRepository.findAll(pageable);
    }

    public EndUserEntity requireBySubject(String subject) {
        return endUserRepository.findBySubject(subject)
                .orElseThrow(() -> DomainException.notFound("User not found"));
    }

    public void requireUsernameAvailable(String username, String currentUsername) {
        if (currentUsername != null && username.equals(currentUsername)) {
            return;
        }
        if (endUserRepository.existsByUsername(username)) {
            throw DomainException.conflict("Username already exists");
        }
    }

    public EndUserEntity create(
            String username,
            boolean enabled,
            boolean admin,
            Instant notBefore
    ) {
        requireUsernameAvailable(username, null);
        EndUserEntity user = new EndUserEntity();
        user.setSubject(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEnabled(enabled);
        user.setAdmin(admin);
        user.setNotBefore(notBefore);
        return endUserRepository.saveAndFlush(user);
    }

    public void updateIdentity(
            EndUserEntity user,
            String username,
            boolean enabled,
            boolean admin,
            Instant notBefore
    ) {
        requireUsernameAvailable(username, user.getUsername());
        user.setUsername(username);
        user.setEnabled(enabled);
        user.setAdmin(admin);
        user.setNotBefore(notBefore);
    }

    public void patchUsername(EndUserEntity user, String username) {
        requireUsernameAvailable(username, user.getUsername());
        user.setUsername(username);
    }

    public void patchEnabled(EndUserEntity user, boolean enabled) {
        user.setEnabled(enabled);
    }

    public void patchAdmin(EndUserEntity user, boolean admin) {
        user.setAdmin(admin);
    }

    public void patchNotBefore(EndUserEntity user, Instant notBefore) {
        user.setNotBefore(notBefore);
    }

    public void forceIncrementVersion(EndUserEntity user) {
        entityManager.lock(user, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    public void flush() {
        entityManager.flush();
    }

    public void delete(EndUserEntity user) {
        endUserRepository.delete(user);
    }

    public String normalizeUsername(String username) {
        return username.trim();
    }
}
