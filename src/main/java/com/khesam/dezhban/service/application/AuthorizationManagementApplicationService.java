package com.khesam.dezhban.service.application;

import com.khesam.dezhban.controller.dto.AuthorizationDtos;
import com.khesam.dezhban.dataaccess.local.entity.AuthorizationRoleEntity;
import com.khesam.dezhban.dataaccess.local.entity.AuthorizationScopeEntity;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.service.domain.authorization.RoleBasedAuthorizationDomainService;
import com.khesam.dezhban.service.domain.client.ClientDomainService;
import com.khesam.dezhban.service.domain.user.EndUserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorizationManagementApplicationService {

    private final RoleBasedAuthorizationDomainService authorizationDomainService;
    private final EndUserDomainService endUserDomainService;
    private final ClientDomainService clientDomainService;

    public AuthorizationManagementApplicationService(
            RoleBasedAuthorizationDomainService authorizationDomainService,
            EndUserDomainService endUserDomainService,
            ClientDomainService clientDomainService
    ) {
        this.authorizationDomainService = authorizationDomainService;
        this.endUserDomainService = endUserDomainService;
        this.clientDomainService = clientDomainService;
    }

    @Transactional(readOnly = true)
    public List<AuthorizationDtos.RoleResponse> listRoles() {
        return authorizationDomainService.listRoles().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorizationDtos.ScopeResponse> listScopes() {
        return authorizationDomainService.listScopes().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuthorizationDtos.RoleResponse createRole(AuthorizationDtos.CreateRoleRequest request) {
        return toResponse(
                authorizationDomainService.createRole(request.code(), request.description())
        );
    }

    @Transactional
    public AuthorizationDtos.ScopeResponse createScope(
            AuthorizationDtos.CreateScopeRequest request
    ) {
        return toResponse(
                authorizationDomainService.createScope(request.code(), request.description())
        );
    }

    @Transactional
    public AuthorizationDtos.RoleResponse replaceRoleScopes(
            String roleCode,
            AuthorizationDtos.ReplaceScopesRequest request
    ) {
        return toResponse(
                authorizationDomainService.replaceRoleScopes(roleCode, request.scopes())
        );
    }

    @Transactional(readOnly = true)
    public AuthorizationDtos.AssignmentResponse getUserAssignment(String subject) {
        EndUserEntity user = endUserDomainService.requireBySubject(subject);
        return new AuthorizationDtos.AssignmentResponse(
                authorizationDomainService.userRoles(user.getId()),
                authorizationDomainService.userPermissions(user.getId()),
                Set.of()
        );
    }

    @Transactional
    public AuthorizationDtos.AssignmentResponse replaceUserRoles(
            String subject,
            AuthorizationDtos.ReplaceRolesRequest request
    ) {
        EndUserEntity user = endUserDomainService.requireBySubject(subject);
        authorizationDomainService.replaceUserRoles(user, request.roles());
        return getUserAssignment(subject);
    }

    @Transactional(readOnly = true)
    public AuthorizationDtos.AssignmentResponse getClientAssignment(String clientId) {
        ClientEntity client = clientDomainService.requireByClientId(clientId);
        return new AuthorizationDtos.AssignmentResponse(
                authorizationDomainService.clientRoles(client.getId()),
                authorizationDomainService.clientPermissions(client.getId()),
                authorizationDomainService.clientScopes(client.getId())
        );
    }

    @Transactional
    public AuthorizationDtos.AssignmentResponse replaceClientRoles(
            String clientId,
            AuthorizationDtos.ReplaceRolesRequest request
    ) {
        ClientEntity client = clientDomainService.requireByClientId(clientId);
        authorizationDomainService.replaceClientRoles(client, request.roles());
        return getClientAssignment(clientId);
    }

    private AuthorizationDtos.RoleResponse toResponse(AuthorizationRoleEntity role) {
        Set<String> scopes = role.getScopes().stream()
                .map(AuthorizationScopeEntity::getCode)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new AuthorizationDtos.RoleResponse(
                role.getCode(),
                role.getDescription(),
                role.isSystemRole(),
                scopes
        );
    }

    private AuthorizationDtos.ScopeResponse toResponse(AuthorizationScopeEntity scope) {
        return new AuthorizationDtos.ScopeResponse(
                scope.getCode(),
                scope.getDescription(),
                scope.isSystemScope()
        );
    }
}
