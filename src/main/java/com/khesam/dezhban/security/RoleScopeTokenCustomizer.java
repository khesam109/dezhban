package com.khesam.dezhban.security;

import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.service.domain.authorization.AuthorizationPolicy;
import com.khesam.dezhban.service.domain.authorization.RoleBasedAuthorizationDomainService;
import com.khesam.dezhban.service.domain.client.ClientDomainService;
import com.khesam.dezhban.service.domain.user.EndUserDomainService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class RoleScopeTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final ClientDomainService clientDomainService;
    private final EndUserDomainService endUserDomainService;
    private final RoleBasedAuthorizationDomainService authorizationDomainService;

    public RoleScopeTokenCustomizer(
            ClientDomainService clientDomainService,
            EndUserDomainService endUserDomainService,
            RoleBasedAuthorizationDomainService authorizationDomainService
    ) {
        this.clientDomainService = clientDomainService;
        this.endUserDomainService = endUserDomainService;
        this.authorizationDomainService = authorizationDomainService;
    }

    @Override
    @Transactional(readOnly = true)
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }

        ClientEntity client = clientDomainService.requireByClientId(
                context.getRegisteredClient().getClientId()
        );
        Set<String> clientRoles = authorizationDomainService.clientRoles(client.getId());
        Set<String> clientPermissions =
                authorizationDomainService.clientPermissions(client.getId());
        Set<String> clientScopes = authorizationDomainService.clientScopes(client.getId());
        Set<String> authorizedBusinessScopes = new LinkedHashSet<>(context.getAuthorizedScopes());
        authorizedBusinessScopes.removeAll(AuthorizationPolicy.PROTOCOL_SCOPES);
        // Never expose more than the client's registered business scopes and role permissions.
        authorizedBusinessScopes.retainAll(clientScopes);
        authorizedBusinessScopes.retainAll(clientPermissions);

        context.getClaims().claim("client_type", client.getClientType().name());
        context.getClaims().claim("client_roles", clientRoles);

        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
                context.getAuthorizationGrantType()
        )) {
            context.getClaims().claim("actor_type", client.getClientType().name());
            context.getClaims().claim("roles", clientRoles);
            context.getClaims().claim("permissions", authorizedBusinessScopes);
            return;
        }

        String username = context.getAuthorization() != null
                ? context.getAuthorization().getPrincipalName()
                : context.getPrincipal().getName();
        EndUserEntity user = endUserDomainService.requireByUsername(username);
        Set<String> userRoles = authorizationDomainService.userRoles(user.getId());
        Set<String> userPermissions = authorizationDomainService.userPermissions(user.getId());

        authorizedBusinessScopes.retainAll(userPermissions);
        context.getClaims().claim("actor_type", "END_USER");
        context.getClaims().claim("roles", userRoles);
        context.getClaims().claim("user_roles", userRoles);
        context.getClaims().claim("permissions", authorizedBusinessScopes);
    }
}
