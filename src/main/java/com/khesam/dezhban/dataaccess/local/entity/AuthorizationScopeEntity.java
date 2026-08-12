package com.khesam.dezhban.dataaccess.local.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "AUTHORIZATION_SCOPE", schema = "DEZHBAN")
public class AuthorizationScopeEntity {

    @Id
    @SequenceGenerator(
            name = "authorizationScopeSequenceGenerator",
            sequenceName = "AUTHORIZATION_SCOPE_SEQ",
            allocationSize = 1,
            schema = "DEZHBAN"
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authorizationScopeSequenceGenerator")
    @Column(name = "ID", nullable = false)
    private long id;

    @Column(name = "CODE", nullable = false, length = 100)
    private String code;

    @Column(name = "DESCRIPTION", nullable = false, length = 500)
    private String description;

    @Column(name = "SYSTEM_SCOPE", nullable = false)
    private boolean systemScope;

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

    public boolean isSystemScope() {
        return systemScope;
    }

    public void setSystemScope(boolean systemScope) {
        this.systemScope = systemScope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
