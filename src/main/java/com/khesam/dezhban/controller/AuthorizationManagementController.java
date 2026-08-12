package com.khesam.dezhban.controller;

import com.khesam.dezhban.controller.dto.AuthorizationDtos;
import com.khesam.dezhban.service.application.AuthorizationManagementApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/authorization")
public class AuthorizationManagementController {

    private final AuthorizationManagementApplicationService authorizationService;

    public AuthorizationManagementController(
            AuthorizationManagementApplicationService authorizationService
    ) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/roles")
    public List<AuthorizationDtos.RoleResponse> listRoles() {
        return authorizationService.listRoles();
    }

    @PostMapping("/roles")
    public ResponseEntity<AuthorizationDtos.RoleResponse> createRole(
            @Valid @RequestBody AuthorizationDtos.CreateRoleRequest request
    ) {
        AuthorizationDtos.RoleResponse response = authorizationService.createRole(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/authorization/roles/" + response.code()))
                .body(response);
    }

    @PutMapping("/roles/{roleCode}/scopes")
    public AuthorizationDtos.RoleResponse replaceRoleScopes(
            @PathVariable String roleCode,
            @Valid @RequestBody AuthorizationDtos.ReplaceScopesRequest request
    ) {
        return authorizationService.replaceRoleScopes(roleCode, request);
    }

    @GetMapping("/scopes")
    public List<AuthorizationDtos.ScopeResponse> listScopes() {
        return authorizationService.listScopes();
    }

    @PostMapping("/scopes")
    public ResponseEntity<AuthorizationDtos.ScopeResponse> createScope(
            @Valid @RequestBody AuthorizationDtos.CreateScopeRequest request
    ) {
        AuthorizationDtos.ScopeResponse response = authorizationService.createScope(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/authorization/scopes/" + response.code()))
                .body(response);
    }

    @GetMapping("/users/{subject}/roles")
    public AuthorizationDtos.AssignmentResponse getUserAssignment(@PathVariable String subject) {
        return authorizationService.getUserAssignment(subject);
    }

    @PutMapping("/users/{subject}/roles")
    public AuthorizationDtos.AssignmentResponse replaceUserRoles(
            @PathVariable String subject,
            @Valid @RequestBody AuthorizationDtos.ReplaceRolesRequest request
    ) {
        return authorizationService.replaceUserRoles(subject, request);
    }

    @GetMapping("/clients/{clientId}/roles")
    public AuthorizationDtos.AssignmentResponse getClientAssignment(
            @PathVariable String clientId
    ) {
        return authorizationService.getClientAssignment(clientId);
    }

    @PutMapping("/clients/{clientId}/roles")
    public AuthorizationDtos.AssignmentResponse replaceClientRoles(
            @PathVariable String clientId,
            @Valid @RequestBody AuthorizationDtos.ReplaceRolesRequest request
    ) {
        return authorizationService.replaceClientRoles(clientId, request);
    }
}
