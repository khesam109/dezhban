package com.khesam.dezhban.dataaccess.local.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "USER_WEBAUTHN_CREDENTIAL", schema = "DEZHBAN")
public class UserWebAuthnCredentialEntity {

    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(
            name = "userWebAuthnCredentialSequenceGenerator",
            sequenceName = "USER_WEBAUTHN_CREDENTIAL_SEQ",
            allocationSize = 1,
            schema = "DEZHBAN"
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userWebAuthnCredentialSequenceGenerator")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false, updatable = false)
    private EndUserEntity endUser;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "CREDENTIAL_ID", nullable = false, length = 500)
    private String credentialId;

    @Column(name = "PUBLIC_KEY", nullable = false, length = 4000)
    private String publicKey;

    @Column(name = "SIGN_COUNT", nullable = false)
    private long signCount;

    @Column(name = "ATTESTATION_FORMAT", nullable = false, length = 100)
    private String attestationFormat;

    @Column(name = "TRANSPORT", length = 255)
    private String transport;

    @Column(name = "IS_DEVICE_BOUND", nullable = false)
    private boolean deviceBound;

    @Column(name = "LABEL", length = 100)
    private String label;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public EndUserEntity getEndUser() {
        return endUser;
    }

    public void setEndUser(EndUserEntity endUser) {
        this.endUser = endUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public long getSignCount() {
        return signCount;
    }

    public void setSignCount(long signCount) {
        this.signCount = signCount;
    }

    public String getAttestationFormat() {
        return attestationFormat;
    }

    public void setAttestationFormat(String attestationFormat) {
        this.attestationFormat = attestationFormat;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public boolean isDeviceBound() {
        return deviceBound;
    }

    public void setDeviceBound(boolean deviceBound) {
        this.deviceBound = deviceBound;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
