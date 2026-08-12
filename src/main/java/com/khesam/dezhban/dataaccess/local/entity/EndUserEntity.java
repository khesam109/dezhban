package com.khesam.dezhban.dataaccess.local.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "END_USER", schema = "DEZHBAN")
public class EndUserEntity {

    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(name = "endUserSequenceGenerator", sequenceName = "END_USER_SEQ", allocationSize = 1, schema = "DEZHBAN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endUserSequenceGenerator")
    private long id;

    @Column(name = "SUBJECT", nullable = false, length = 36)
    private String subject;

    @Column(name = "USERNAME", nullable = false, length = 255)
    private String username;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;

    @Column(name = "ADMIN", nullable = false)
    private boolean admin;

    @Column(name = "LOCKED", nullable = false)
    private boolean locked;

    @Column(name = "LOCK_UNTIL")
    private Instant lockUntil;

    @Column(name = "FAILED_LOGIN_ATTEMPTS", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "FAILED_LOGIN_AT")
    private Instant failedLoginAt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "MODIFIED_AT", insertable = false)
    @UpdateTimestamp
    private Instant modifiedAt;

    @Column(name = "NOT_BEFORE")
    private Instant notBefore;

    @Column(name = "WEBAUTHN_HANDLE", length = 64)
    private String webAuthnHandle;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Instant getLockUntil() {
        return lockUntil;
    }

    public void setLockUntil(Instant lockUntil) {
        this.lockUntil = lockUntil;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getFailedLoginAt() {
        return failedLoginAt;
    }

    public void setFailedLoginAt(Instant failedLoginAt) {
        this.failedLoginAt = failedLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Instant notBefore) {
        this.notBefore = notBefore;
    }

    public long getVersion() {
        return version;
    }

    public String getWebAuthnHandle() {
        return webAuthnHandle;
    }

    public void setWebAuthnHandle(String webAuthnHandle) {
        this.webAuthnHandle = webAuthnHandle;
    }
}
