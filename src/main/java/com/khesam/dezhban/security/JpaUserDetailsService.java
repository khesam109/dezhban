package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserPasswordCredentialEntity;
import com.khesam.dezhban.dataaccess.local.repository.UserPasswordCredentialRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserPasswordCredentialRepository credentialRepository;

    public JpaUserDetailsService(UserPasswordCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserPasswordCredentialEntity credential = credentialRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        EndUserEntity endUser = credential.getEndUser();
        Instant now = Instant.now();

        boolean disabled = !endUser.isEnabled()
                || endUser.getNotBefore() != null && endUser.getNotBefore().isAfter(now);
        boolean accountLocked = endUser.isLocked()
                && (endUser.getLockUntil() == null || endUser.getLockUntil().isAfter(now));
        boolean credentialsExpired = credential.getExpiresAt() != null
                && !credential.getExpiresAt().isAfter(now);

        return User.withUsername(endUser.getUsername())
                .password(credential.getPasswordHash())
                .authorities("ROLE_USER")
                .disabled(disabled)
                .accountLocked(accountLocked)
                .credentialsExpired(credentialsExpired)
                .build();
    }
}
