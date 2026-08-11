package com.khesam.dezhban.security;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class OAuth2AuthorizationCodec {

    private static final long MAX_SERIALIZED_BYTES = 5 * 1024 * 1024;

    public byte[] encode(OAuth2Authorization authorization) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(authorization);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize OAuth2 authorization", exception);
        }
    }

    public OAuth2Authorization decode(byte[] authorizationData) {
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(authorizationData)
        )) {
            input.setObjectInputFilter(this::filter);
            return (OAuth2Authorization) input.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Could not deserialize OAuth2 authorization", exception);
        }
    }

    private ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo filterInfo) {
        if (filterInfo.depth() > 100
                || filterInfo.references() > 10_000
                || filterInfo.streamBytes() > MAX_SERIALIZED_BYTES) {
            return ObjectInputFilter.Status.REJECTED;
        }

        Class<?> serializedClass = filterInfo.serialClass();
        if (serializedClass == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (serializedClass.isArray()) {
            serializedClass = serializedClass.getComponentType();
        }
        if (serializedClass.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }

        String className = serializedClass.getName();
        if (className.startsWith("java.lang.")
                || className.startsWith("java.math.")
                || className.startsWith("java.net.")
                || className.startsWith("java.time.")
                || className.startsWith("java.util.")
                || className.startsWith("org.springframework.security.")) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }
}
