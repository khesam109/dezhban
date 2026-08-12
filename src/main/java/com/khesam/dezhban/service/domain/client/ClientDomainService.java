package com.khesam.dezhban.service.domain.client;

import com.khesam.dezhban.common.ClientAuthenticationType;
import com.khesam.dezhban.common.ClientType;
import com.khesam.dezhban.common.GrantType;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.repository.ClientRepository;
import com.khesam.dezhban.service.domain.support.DomainException;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Service
public class ClientDomainService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientDomainService(
            ClientRepository clientRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager
    ) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    public Page<ClientEntity> list(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    public ClientEntity requireByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .orElseThrow(() -> DomainException.notFound("Client not found"));
    }

    public void requireClientIdAvailable(String clientId) {
        if (clientRepository.existsByClientId(clientId)) {
            throw DomainException.conflict("Client ID already exists");
        }
    }

    public CreatedClient create(CreateCommand command) {
        requireClientIdAvailable(command.clientId());
        validateConfiguration(command.configuration());

        String rawSecret = null;
        if (!command.publicClient()) {
            rawSecret = command.clientSecret() == null || command.clientSecret().isBlank()
                    ? generateSecret()
                    : command.clientSecret();
        }

        ClientEntity client = new ClientEntity();
        client.setClientId(command.clientId());
        client.setEnabled(command.enabled());
        client.setPublicClient(command.publicClient());
        client.setSecretHash(rawSecret == null ? null : passwordEncoder.encode(rawSecret));
        client.setSecretExpiresAt(command.secretExpiresAt());
        applyConfiguration(client, command.configuration());
        clientRepository.saveAndFlush(client);
        return new CreatedClient(client, rawSecret);
    }

    public void replace(ClientEntity client, ReplaceCommand command) {
        validateConfiguration(command.configuration());
        if (!command.publicClient() && client.getSecretHash() == null) {
            throw DomainException.invalid("A confidential client requires a client secret");
        }
        client.setEnabled(command.enabled());
        client.setPublicClient(command.publicClient());
        if (command.publicClient()) {
            client.setSecretHash(null);
            client.setSecretExpiresAt(null);
        } else {
            client.setSecretExpiresAt(command.secretExpiresAt());
        }
        applyConfiguration(client, command.configuration());
    }

    public void patchEnabled(ClientEntity client, boolean enabled) {
        client.setEnabled(enabled);
    }

    public void patchNotBefore(ClientEntity client, Instant notBefore) {
        client.setNotBefore(notBefore);
    }

    public void patchSecretExpiresAt(ClientEntity client, Instant secretExpiresAt) {
        client.setSecretExpiresAt(secretExpiresAt);
    }

    public String rotateSecret(ClientEntity client, Instant expiresAt) {
        if (client.isPublicClient()) {
            throw DomainException.invalid("Public clients do not have shared secrets");
        }
        String rawSecret = generateSecret();
        client.setSecretHash(passwordEncoder.encode(rawSecret));
        client.setSecretExpiresAt(expiresAt);
        return rawSecret;
    }

    public void flush() {
        entityManager.flush();
    }

    public void delete(ClientEntity client) {
        clientRepository.delete(client);
    }

    public void validateConfiguration(ClientConfiguration configuration) {
        if (configuration.publicClient()) {
            if (!configuration.authenticationMethods().equals(Set.of(ClientAuthenticationType.NONE))) {
                throw DomainException.invalid("Public clients must use only the NONE authentication method");
            }
            if (!configuration.requireProofKey()) {
                throw DomainException.invalid("Public authorization-code clients must require PKCE");
            }
            if (configuration.grantTypes().contains(GrantType.CLIENT_CREDENTIALS)) {
                throw DomainException.invalid("Public clients cannot use the client_credentials grant");
            }
        } else if (configuration.authenticationMethods().contains(ClientAuthenticationType.NONE)) {
            throw DomainException.invalid("Confidential clients cannot use the NONE authentication method");
        }
        if (configuration.grantTypes().contains(GrantType.AUTHORIZATION_CODE)
                && configuration.redirectUris().isEmpty()) {
            throw DomainException.invalid("Authorization-code clients require a redirect URI");
        }
        if (configuration.clientType() == ClientType.AP && !configuration.apProfilePresent()) {
            throw DomainException.invalid("AP clients require an AP profile");
        }
        if (configuration.clientType() != ClientType.AP && configuration.apProfilePresent()) {
            throw DomainException.invalid("Only AP clients may have an AP profile");
        }
        if (configuration.redirectUris().stream().anyMatch(uri -> uri.contains("#"))) {
            throw DomainException.invalid("Redirect URIs must not contain fragments");
        }
        if (configuration.authenticationMethods().toString().length() > 255
                || configuration.grantTypes().toString().length() > 255
                || String.join(",", configuration.redirectUris()).length() > 1000) {
            throw DomainException.invalid("Serialized client configuration exceeds database limits");
        }
    }

    private void applyConfiguration(ClientEntity client, ClientConfiguration configuration) {
        client.setClientType(configuration.clientType());
        client.setAuthenticationMethods(Set.copyOf(configuration.authenticationMethods()));
        client.setGrantTypes(Set.copyOf(configuration.grantTypes()));
        client.setRedirectUris(Set.copyOf(configuration.redirectUris()));
        client.setPostLogoutRedirectUri(configuration.postLogoutRedirectUri());
        client.setScopes(Set.copyOf(configuration.scopes()));
        client.setClientSettings(
                "{\"requireProofKey\":" + configuration.requireProofKey()
                        + ",\"requireAuthorizationConsent\":"
                        + configuration.requireAuthorizationConsent() + "}"
        );
        client.setTokenSettings(configuration.tokenSettingsJson());
        client.setNotBefore(configuration.notBefore());
    }

    private String generateSecret() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public record ClientConfiguration(
            boolean publicClient,
            ClientType clientType,
            Set<ClientAuthenticationType> authenticationMethods,
            Set<GrantType> grantTypes,
            Set<String> redirectUris,
            String postLogoutRedirectUri,
            Set<String> scopes,
            boolean requireProofKey,
            boolean requireAuthorizationConsent,
            String tokenSettingsJson,
            Instant notBefore,
            boolean apProfilePresent
    ) {
    }

    public record CreateCommand(
            String clientId,
            boolean enabled,
            boolean publicClient,
            String clientSecret,
            Instant secretExpiresAt,
            ClientConfiguration configuration
    ) {
    }

    public record ReplaceCommand(
            boolean enabled,
            boolean publicClient,
            Instant secretExpiresAt,
            ClientConfiguration configuration
    ) {
    }

    public record CreatedClient(ClientEntity client, String rawSecret) {
    }
}
