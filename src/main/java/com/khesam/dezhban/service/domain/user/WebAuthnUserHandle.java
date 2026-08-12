package com.khesam.dezhban.service.domain.user;

import com.khesam.dezhban.service.domain.support.DomainException;

import java.util.UUID;

/**
 * Stable WebAuthn user handle. Unlike the human-readable username, this value
 * is suitable for {@code user.id} and {@code allowCredentials} without exposing
 * PII and without breaking credentials when usernames change.
 */
public final class WebAuthnUserHandle {

    private WebAuthnUserHandle() {
    }

    public static final int BYTE_LENGTH = 32;

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static byte[] toBytes(String handle) {
        String hex = requireValid(handle);
        byte[] bytes = new byte[BYTE_LENGTH];
        for (int i = 0; i < BYTE_LENGTH; i++) {
            bytes[i] = (byte) Integer.parseInt(hex, i * 2, i * 2 + 2, 16);
        }
        return bytes;
    }

    public static String fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != BYTE_LENGTH) {
            throw DomainException.invalid("WebAuthn user handle has an unexpected length");
        }
        StringBuilder hex = new StringBuilder(BYTE_LENGTH * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    private static String requireValid(String handle) {
        if (handle == null || !handle.matches("[0-9a-fA-F]{64}")) {
            throw DomainException.invalid("WebAuthn user handle is not a valid 32-byte value");
        }
        return handle.toLowerCase();
    }
}
