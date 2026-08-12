package com.khesam.dezhban.service.domain.user;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserPasswordCredentialEntity;
import com.khesam.dezhban.dataaccess.local.repository.UserPasswordCredentialRepository;
import com.khesam.dezhban.service.domain.support.DomainException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class UserPasswordCredentialDomainService {

    private final UserPasswordCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    public UserPasswordCredentialDomainService(
            UserPasswordCredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void create(EndUserEntity user, String rawPassword) {
        validatePassword(rawPassword);
        UserPasswordCredentialEntity credential = new UserPasswordCredentialEntity();
        credential.setEndUser(user);
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credentialRepository.save(credential);
    }

    public void updatePassword(EndUserEntity user, String rawPassword, Instant expiresAt) {
        validatePassword(rawPassword);
        UserPasswordCredentialEntity credential = credentialRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserPasswordCredentialEntity value = new UserPasswordCredentialEntity();
                    value.setEndUser(user);
                    return value;
                });
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setExpiresAt(expiresAt);
        credentialRepository.save(credential);
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw DomainException.invalid("Password must not exceed BCrypt's 72-byte input limit");
        }
    }
}
