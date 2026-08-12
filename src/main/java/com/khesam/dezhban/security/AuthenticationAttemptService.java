package com.khesam.dezhban.security;

import com.khesam.dezhban.common.AuthenticationActorType;
import com.khesam.dezhban.common.AuthenticationOutcome;
import com.khesam.dezhban.config.AuthenticationLockoutProperties;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.LoginEventEntity;
import com.khesam.dezhban.dataaccess.local.repository.ClientRepository;
import com.khesam.dezhban.dataaccess.local.repository.EndUserRepository;
import com.khesam.dezhban.dataaccess.local.repository.LoginEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthenticationAttemptService {

    private final EndUserRepository endUserRepository;
    private final ClientRepository clientRepository;
    private final LoginEventRepository loginEventRepository;
    private final AuthenticationLockoutProperties properties;

    public AuthenticationAttemptService(
            EndUserRepository endUserRepository,
            ClientRepository clientRepository,
            LoginEventRepository loginEventRepository,
            AuthenticationLockoutProperties properties
    ) {
        this.endUserRepository = endUserRepository;
        this.clientRepository = clientRepository;
        this.loginEventRepository = loginEventRepository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserSuccess(String username, RequestDetails details) {
        Optional<EndUserEntity> endUser =
                endUserRepository.findForUpdateByUsername(username);
        endUser.ifPresent(this::resetUserFailures);
        saveEvent(
                AuthenticationActorType.USER,
                AuthenticationOutcome.SUCCESS,
                username,
                endUser.map(EndUserEntity::getId).orElse(null),
                null,
                "FORM_PASSWORD",
                null,
                details
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserBadCredentials(String username, RequestDetails details) {
        Instant now = Instant.now();
        Optional<EndUserEntity> endUser =
                endUserRepository.findForUpdateByUsername(username);
        AuthenticationOutcome outcome = endUser
                .map(user -> registerUserFailure(user, now))
                .orElse(AuthenticationOutcome.FAILURE);
        saveEvent(
                AuthenticationActorType.USER,
                outcome,
                username,
                endUser.map(EndUserEntity::getId).orElse(null),
                null,
                "FORM_PASSWORD",
                "BAD_CREDENTIALS",
                details
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserRejection(
            String username,
            String reason,
            boolean locked,
            RequestDetails details
    ) {
        Optional<EndUserEntity> endUser = endUserRepository.findByUsername(username);
        saveEvent(
                AuthenticationActorType.USER,
                locked ? AuthenticationOutcome.LOCKED : AuthenticationOutcome.FAILURE,
                username,
                endUser.map(EndUserEntity::getId).orElse(null),
                null,
                "FORM_PASSWORD",
                reason,
                details
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordClientSuccess(
            String clientId,
            String authenticationMethod,
            RequestDetails details
    ) {
        Optional<ClientEntity> client = clientRepository.findForUpdateByClientId(clientId);
        client.ifPresent(this::resetClientFailures);
        saveEvent(
                AuthenticationActorType.CLIENT,
                AuthenticationOutcome.SUCCESS,
                clientId,
                null,
                client.map(ClientEntity::getId).orElse(null),
                authenticationMethod,
                null,
                details
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordClientBadCredentials(
            String clientId,
            String authenticationMethod,
            RequestDetails details
    ) {
        Instant now = Instant.now();
        Optional<ClientEntity> client = clientRepository.findForUpdateByClientId(clientId);
        AuthenticationOutcome outcome = client
                .map(value -> registerClientFailure(value, now))
                .orElse(AuthenticationOutcome.FAILURE);
        saveEvent(
                AuthenticationActorType.CLIENT,
                outcome,
                clientId,
                null,
                client.map(ClientEntity::getId).orElse(null),
                authenticationMethod,
                "INVALID_CLIENT",
                details
        );
    }

    private AuthenticationOutcome registerUserFailure(EndUserEntity user, Instant now) {
        if (isActivelyLocked(user.isLocked(), user.getLockUntil(), now)) {
            return AuthenticationOutcome.LOCKED;
        }
        clearExpiredUserLock(user, now);
        int attempts = nextAttempts(
                user.getFailedLoginAttempts(),
                user.getFailedLoginAt(),
                now,
                properties.getUserObservationWindow()
        );
        user.setFailedLoginAttempts(attempts);
        user.setFailedLoginAt(now);
        if (attempts >= properties.getUserMaxAttempts()) {
            user.setLocked(true);
            user.setLockUntil(now.plus(properties.getUserLockDuration()));
            return AuthenticationOutcome.LOCKED;
        }
        return AuthenticationOutcome.FAILURE;
    }

    private AuthenticationOutcome registerClientFailure(ClientEntity client, Instant now) {
        if (isActivelyLocked(client.isLocked(), client.getLockUntil(), now)) {
            return AuthenticationOutcome.LOCKED;
        }
        clearExpiredClientLock(client, now);
        int attempts = nextAttempts(
                client.getFailedAuthenticationAttempts(),
                client.getFailedAuthenticationAt(),
                now,
                properties.getClientObservationWindow()
        );
        client.setFailedAuthenticationAttempts(attempts);
        client.setFailedAuthenticationAt(now);
        if (attempts >= properties.getClientMaxAttempts()) {
            client.setLocked(true);
            client.setLockUntil(now.plus(properties.getClientLockDuration()));
            return AuthenticationOutcome.LOCKED;
        }
        return AuthenticationOutcome.FAILURE;
    }

    private int nextAttempts(
            int previousAttempts,
            Instant previousFailure,
            Instant now,
            Duration observationWindow
    ) {
        if (previousFailure == null || previousFailure.isBefore(now.minus(observationWindow))) {
            return 1;
        }
        return previousAttempts + 1;
    }

    private boolean isActivelyLocked(boolean locked, Instant lockUntil, Instant now) {
        return locked && (lockUntil == null || lockUntil.isAfter(now));
    }

    private void clearExpiredUserLock(EndUserEntity user, Instant now) {
        if (user.isLocked() && user.getLockUntil() != null && !user.getLockUntil().isAfter(now)) {
            user.setLocked(false);
            user.setLockUntil(null);
        }
    }

    private void clearExpiredClientLock(ClientEntity client, Instant now) {
        if (client.isLocked()
                && client.getLockUntil() != null
                && !client.getLockUntil().isAfter(now)) {
            client.setLocked(false);
            client.setLockUntil(null);
        }
    }

    private void resetUserFailures(EndUserEntity user) {
        user.setFailedLoginAttempts(0);
        user.setFailedLoginAt(null);
        clearExpiredUserLock(user, Instant.now());
    }

    private void resetClientFailures(ClientEntity client) {
        client.setFailedAuthenticationAttempts(0);
        client.setFailedAuthenticationAt(null);
        clearExpiredClientLock(client, Instant.now());
    }

    private void saveEvent(
            AuthenticationActorType actorType,
            AuthenticationOutcome outcome,
            String identifier,
            Long endUserId,
            Long clientId,
            String authenticationMethod,
            String failureReason,
            RequestDetails details
    ) {
        LoginEventEntity event = new LoginEventEntity();
        event.setOccurredAt(Instant.now());
        event.setActorType(actorType);
        event.setOutcome(outcome);
        event.setPresentedIdentifier(limit(identifier, 255, "<unknown>"));
        event.setEndUserId(endUserId);
        event.setClientId(clientId);
        event.setAuthenticationMethod(limit(authenticationMethod, 100, "UNKNOWN"));
        event.setFailureReason(limit(failureReason, 100, null));
        event.setRemoteAddress(limit(details.remoteAddress(), 45, null));
        event.setSessionId(limit(details.sessionId(), 100, null));
        loginEventRepository.save(event);
    }

    private String limit(String value, int maximumLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }

    public record RequestDetails(String remoteAddress, String sessionId) {
        public static final RequestDetails EMPTY = new RequestDetails(null, null);
    }
}
