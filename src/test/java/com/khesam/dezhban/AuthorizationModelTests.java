package com.khesam.dezhban;

import com.khesam.dezhban.common.ClientType;
import com.khesam.dezhban.dataaccess.local.repository.ClientRepository;
import com.khesam.dezhban.dataaccess.local.repository.EndUserRepository;
import com.khesam.dezhban.service.domain.authorization.AuthorizationPolicy;
import com.khesam.dezhban.service.domain.authorization.RoleBasedAuthorizationDomainService;
import com.khesam.dezhban.service.domain.support.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthorizationModelTests {

    @Autowired
    private RoleBasedAuthorizationDomainService authorizationDomainService;

    @Autowired
    private EndUserRepository endUserRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void seededUserReceivesEndUserAndAdminRoles() {
        var user = endUserRepository.findByUsername("user").orElseThrow();
        assertThat(authorizationDomainService.userRoles(user.getId()))
                .containsExactlyInAnyOrder(AuthorizationPolicy.END_USER, AuthorizationPolicy.ADMIN);
        assertThat(authorizationDomainService.userPermissions(user.getId()))
                .contains("system:admin", "sign_request:sign", "certificate:issue");
    }

    @Test
    void seededApClientReceivesActorRoleAndBusinessScopes() {
        var client = clientRepository.findByClientId("client").orElseThrow();
        assertThat(client.getClientType()).isEqualTo(ClientType.AP);
        assertThat(authorizationDomainService.clientRoles(client.getId()))
                .containsExactly("AP");
        assertThat(authorizationDomainService.clientScopes(client.getId()))
                .containsExactlyInAnyOrder("openid", "sign_request:create", "sign_request:read");
        assertThat(authorizationDomainService.clientPermissions(client.getId()))
                .containsExactlyInAnyOrder("sign_request:create", "sign_request:read");
    }

    @Test
    void thirdPartyMobileCannotRequestFirstPartyScopes() {
        var client = clientRepository.findByClientId("client").orElseThrow();
        assertThatThrownBy(() -> authorizationDomainService.synchronizeClientAuthorization(
                client,
                ClientType.MOBILE_THIRD_PARTY,
                Set.of("openid", "sign_request:sign")
        )).isInstanceOf(DomainException.class)
                .hasMessageContaining("sign_request:sign");
    }
}
