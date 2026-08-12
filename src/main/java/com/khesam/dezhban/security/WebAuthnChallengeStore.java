package com.khesam.dezhban.security;

import com.khesam.dezhban.service.domain.support.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WebAuthnChallengeStore {

    private static final long CHALLENGE_TTL_MS = java.time.Duration.ofMinutes(5).toMillis();

    private final Map<String, Challenge> challenges = new LinkedHashMap<>();

    public synchronized String issue(String username, Object options) {
        purgeExpired();
        String id = UUID.randomUUID().toString();
        challenges.put(id, new Challenge(username, options, System.currentTimeMillis()));
        return id;
    }

    public synchronized Challenge consume(String challengeId, String username) {
        purgeExpired();
        Challenge challenge = challenges.remove(challengeId);
        if (challenge == null || !challenge.username().equals(username)) {
            throw DomainException.invalid("WebAuthn challenge is unknown, expired, or does not belong to this user");
        }
        return challenge;
    }

    private void purgeExpired() {
        long cutoff = System.currentTimeMillis() - CHALLENGE_TTL_MS;
        challenges.entrySet().removeIf(entry -> entry.getValue().issuedAt() < cutoff);
    }

    public record Challenge(String username, Object options, long issuedAt) {
    }
}
