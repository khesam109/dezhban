package com.khesam.dezhban.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class AuthorizationDtos {

    private AuthorizationDtos() {
    }

    public record CreateRoleRequest(
            @NotBlank
            @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{1,99}")
            String code,
            @NotBlank @Size(max = 500) String description
    ) {
    }

    public record CreateScopeRequest(
            @NotBlank
            @Pattern(regexp = "[a-z][a-z0-9_.:-]{1,99}")
            String code,
            @NotBlank @Size(max = 500) String description
    ) {
    }

    public record ReplaceScopesRequest(
            @NotEmpty Set<
                    @Pattern(regexp = "[a-z][a-z0-9_.:-]{1,99}") String
                    > scopes
    ) {
    }

    public record ReplaceRolesRequest(
            @NotEmpty Set<
                    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{1,99}") String
                    > roles
    ) {
    }

    public record ScopeResponse(
            String code,
            String description,
            boolean system
    ) {
    }

    public record RoleResponse(
            String code,
            String description,
            boolean system,
            Set<String> scopes
    ) {
    }

    public record AssignmentResponse(
            Set<String> roles,
            Set<String> permissions,
            Set<String> clientScopes
    ) {
    }
}
