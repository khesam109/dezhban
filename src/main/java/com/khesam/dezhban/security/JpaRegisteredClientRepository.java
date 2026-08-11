package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.repository.ClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientRepository clientRepository;
    private final RegisteredClientMapper registeredClientMapper;

    public JpaRegisteredClientRepository(
            ClientRepository clientRepository,
            RegisteredClientMapper registeredClientMapper
    ) {
        this.clientRepository = clientRepository;
        this.registeredClientMapper = registeredClientMapper;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException(
                "Client registration is read-only until client management is implemented"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        try {
            return clientRepository.findById(Long.parseLong(id))
                    .filter(this::isActive)
                    .map(registeredClientMapper::toRegisteredClient)
                    .orElse(null);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .filter(this::isActive)
                .map(registeredClientMapper::toRegisteredClient)
                .orElse(null);
    }

    private boolean isActive(ClientEntity client) {
        return client.isEnabled()
                && (client.getNotBefore() == null || !client.getNotBefore().isAfter(Instant.now()));
    }
}
