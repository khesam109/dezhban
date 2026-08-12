package com.khesam.dezhban.service.domain.authorization;

import com.khesam.dezhban.common.ClientType;
import com.khesam.dezhban.dataaccess.local.entity.AuthorizationRoleEntity;
import com.khesam.dezhban.dataaccess.local.entity.AuthorizationScopeEntity;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.repository.AuthorizationAssignmentRepository;
import com.khesam.dezhban.dataaccess.local.repository.AuthorizationRoleRepository;
import com.khesam.dezhban.dataaccess.local.repository.AuthorizationScopeRepository;
import com.khesam.dezhban.service.domain.support.DomainException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleBasedAuthorizationDomainService {

    private final AuthorizationRoleRepository roleRepository;
    private final AuthorizationScopeRepository scopeRepository;
    private final AuthorizationAssignmentRepository assignmentRepository;

    public RoleBasedAuthorizationDomainService(
            AuthorizationRoleRepository roleRepository,
            AuthorizationScopeRepository scopeRepository,
            AuthorizationAssignmentRepository assignmentRepository
    ) {
        this.roleRepository = roleRepository;
        this.scopeRepository = scopeRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<AuthorizationRoleEntity> listRoles() {
        return roleRepository.findAllByOrderByCodeAsc();
    }

    public List<AuthorizationScopeEntity> listScopes() {
        return scopeRepository.findAllByOrderByCodeAsc();
    }

    public AuthorizationRoleEntity createRole(String code, String description) {
        String normalized = normalizeRole(code);
        if (roleRepository.findByCode(normalized).isPresent()) {
            throw DomainException.conflict("Role already exists");
        }
        AuthorizationRoleEntity role = new AuthorizationRoleEntity();
        role.setCode(normalized);
        role.setDescription(description.trim());
        role.setSystemRole(false);
        return roleRepository.save(role);
    }

    public AuthorizationScopeEntity createScope(String code, String description) {
        String normalized = normalizeScope(code);
        if (scopeRepository.findByCode(normalized).isPresent()) {
            throw DomainException.conflict("Scope already exists");
        }
        AuthorizationScopeEntity scope = new AuthorizationScopeEntity();
        scope.setCode(normalized);
        scope.setDescription(description.trim());
        scope.setSystemScope(false);
        return scopeRepository.save(scope);
    }

    public AuthorizationRoleEntity replaceRoleScopes(String roleCode, Set<String> scopeCodes) {
        AuthorizationRoleEntity role = requireRole(roleCode);
        if (role.isSystemRole()) {
            throw DomainException.invalid("System role permissions cannot be changed");
        }
        role.setScopes(new LinkedHashSet<>(requireScopes(scopeCodes)));
        return role;
    }

    public void synchronizeUserRoles(EndUserEntity user) {
        Set<String> roles = new LinkedHashSet<>(assignmentRepository.findUserRoles(user.getId()));
        roles.add(AuthorizationPolicy.END_USER);
        if (user.isAdmin()) {
            roles.add(AuthorizationPolicy.ADMIN);
        } else {
            roles.remove(AuthorizationPolicy.ADMIN);
        }
        replaceUserRoles(user, roles);
    }

    public void replaceUserRoles(EndUserEntity user, Set<String> roleCodes) {
        Set<String> normalized = roleCodes.stream()
                .map(this::normalizeRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        normalized.add(AuthorizationPolicy.END_USER);
        if (user.isAdmin()) {
            normalized.add(AuthorizationPolicy.ADMIN);
        } else {
            normalized.remove(AuthorizationPolicy.ADMIN);
        }
        assignmentRepository.replaceUserRoles(user.getId(), roleIds(normalized));
    }

    public void synchronizeClientAuthorization(
            ClientEntity client,
            ClientType type,
            Set<String> requestedScopes
    ) {
        Set<String> currentRoles = new LinkedHashSet<>(
                assignmentRepository.findClientRoles(client.getId())
        );
        currentRoles.removeAll(AuthorizationPolicy.CLIENT_ACTOR_ROLES);
        currentRoles.add(AuthorizationPolicy.actorRole(type));

        Set<String> availablePermissions = permissionsForRoles(currentRoles);
        validateClientScopes(type, requestedScopes, availablePermissions);
        assignmentRepository.replaceClientRoles(client.getId(), roleIds(currentRoles));
        assignmentRepository.replaceClientScopes(client.getId(), scopeIds(requestedScopes));
    }

    public void replaceClientRoles(ClientEntity client, Set<String> roleCodes) {
        Set<String> normalized = roleCodes.stream()
                .map(this::normalizeRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        normalized.removeAll(AuthorizationPolicy.CLIENT_ACTOR_ROLES);
        normalized.add(AuthorizationPolicy.actorRole(client.getClientType()));
        Set<String> availablePermissions = permissionsForRoles(normalized);
        validateClientScopes(client.getClientType(), client.getScopes(), availablePermissions);
        assignmentRepository.replaceClientRoles(client.getId(), roleIds(normalized));
    }

    public Set<String> userRoles(long userId) {
        return assignmentRepository.findUserRoles(userId);
    }

    public Set<String> userPermissions(long userId) {
        return assignmentRepository.findUserPermissions(userId);
    }

    public Set<String> clientRoles(long clientId) {
        return assignmentRepository.findClientRoles(clientId);
    }

    public Set<String> clientPermissions(long clientId) {
        return assignmentRepository.findClientPermissions(clientId);
    }

    public Set<String> clientScopes(long clientId) {
        return assignmentRepository.findClientScopes(clientId);
    }

    private void validateClientScopes(
            ClientType type,
            Set<String> requestedScopes,
            Set<String> rolePermissions
    ) {
        requireScopes(requestedScopes);
        Set<String> allowed = new LinkedHashSet<>(AuthorizationPolicy.PROTOCOL_SCOPES);
        allowed.addAll(AuthorizationPolicy.businessScopes(type));
        allowed.addAll(rolePermissions);
        Set<String> forbidden = requestedScopes.stream()
                .filter(scope -> !allowed.contains(scope))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!forbidden.isEmpty()) {
            throw DomainException.invalid(
                    "Client type " + type + " cannot use scopes: " + String.join(", ", forbidden)
            );
        }
    }

    private AuthorizationRoleEntity requireRole(String code) {
        return roleRepository.findByCode(normalizeRole(code))
                .orElseThrow(() -> DomainException.notFound("Role not found: " + code));
    }

    private List<AuthorizationRoleEntity> requireRoles(Set<String> codes) {
        List<AuthorizationRoleEntity> roles = roleRepository.findAllByCodeIn(codes);
        Set<String> found = roles.stream()
                .map(AuthorizationRoleEntity::getCode)
                .collect(Collectors.toSet());
        if (!found.equals(codes)) {
            Set<String> missing = new LinkedHashSet<>(codes);
            missing.removeAll(found);
            throw DomainException.notFound("Roles not found: " + String.join(", ", missing));
        }
        return roles;
    }

    private List<AuthorizationScopeEntity> requireScopes(Set<String> codes) {
        Set<String> normalized = codes.stream()
                .map(this::normalizeScope)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<AuthorizationScopeEntity> scopes = scopeRepository.findAllByCodeIn(normalized);
        Set<String> found = scopes.stream()
                .map(AuthorizationScopeEntity::getCode)
                .collect(Collectors.toSet());
        if (!found.equals(normalized)) {
            Set<String> missing = new LinkedHashSet<>(normalized);
            missing.removeAll(found);
            throw DomainException.notFound("Scopes not found: " + String.join(", ", missing));
        }
        return scopes;
    }

    private Set<Long> roleIds(Set<String> codes) {
        return requireRoles(codes).stream()
                .map(AuthorizationRoleEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> scopeIds(Set<String> codes) {
        return requireScopes(codes).stream()
                .map(AuthorizationScopeEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> permissionsForRoles(Set<String> roles) {
        return requireRoles(roles).stream()
                .flatMap(role -> role.getScopes().stream())
                .map(AuthorizationScopeEntity::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeRole(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeScope(String code) {
        return code.trim().toLowerCase();
    }
}
