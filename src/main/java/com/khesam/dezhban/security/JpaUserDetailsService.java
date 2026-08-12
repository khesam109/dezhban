package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserPasswordCredentialEntity;
import com.khesam.dezhban.dataaccess.local.repository.UserPasswordCredentialRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class JpaUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

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

        User.UserBuilder user = User.withUsername(endUser.getUsername())
                .password(credential.getPasswordHash())
                .disabled(disabled)
                .accountLocked(accountLocked)
                .credentialsExpired(credentialsExpired);
        user.authorities(endUser.isAdmin()
                ? new String[]{"ROLE_USER", "ROLE_ADMIN"}
                : new String[]{"ROLE_USER"});
        return user.build();
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, @Nullable String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New encoded password must not be empty");
        }
        UserPasswordCredentialEntity credential =
                credentialRepository.findByUsername(user.getUsername())
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found: " + user.getUsername()
                        ));
        credential.setPasswordHash(newPassword);
        return User.withUserDetails(user).password(newPassword).build();
    }
}
