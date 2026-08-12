package com.khesam.dezhban.service.domain.authorization;

import com.khesam.dezhban.dataaccess.local.repository.OAuth2AuthorizationRepository;
import org.springframework.stereotype.Service;

@Service
public class OAuth2AuthorizationDomainService {

    private final OAuth2AuthorizationRepository authorizationRepository;

    public OAuth2AuthorizationDomainService(OAuth2AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    public void revokeByPrincipalName(String principalName) {
        authorizationRepository.deleteAllByPrincipalName(principalName);
    }

    public void revokeByRegisteredClientId(String registeredClientId) {
        authorizationRepository.deleteAllByRegisteredClientId(registeredClientId);
    }
}
