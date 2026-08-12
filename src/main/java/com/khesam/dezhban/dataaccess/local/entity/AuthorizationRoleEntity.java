package com.khesam.dezhban.dataaccess.local.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "AUTHORIZATION_ROLE", schema = "DEZHBAN")
public class AuthorizationRoleEntity {

    @Id
    @SequenceGenerator(
            name = "authorizationRoleSequenceGenerator",
            sequenceName = "AUTHORIZATION_ROLE_SEQ",
            allocationSize = 1,
            schema = "DEZHBAN"
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authorizationRoleSequenceGenerator")
    @Column(name = "ID", nullable = false)
    private long id;

    @Column(name = "CODE", nullable = false, length = 100)
    private String code;

    @Column(name = "DESCRIPTION", nullable = false, length = 500)
    private String description;

    @Column(name = "SYSTEM_ROLE", nullable = false)
    private boolean systemRole;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "AUTHORIZATION_ROLE_SCOPE",
            schema = "DEZHBAN",
            joinColumns = @JoinColumn(name = "ROLE_ID"),
            inverseJoinColumns = @JoinColumn(name = "SCOPE_ID")
    )
    private Set<AuthorizationScopeEntity> scopes = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public void setSystemRole(boolean systemRole) {
        this.systemRole = systemRole;
    }

    public Set<AuthorizationScopeEntity> getScopes() {
        return scopes;
    }

    public void setScopes(Set<AuthorizationScopeEntity> scopes) {
        this.scopes = scopes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
