package com.khesam.dezhban.dataaccess.local.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "AP_CLIENT_PROFILE", schema = "DEZHBAN")
public class ApClientProfileEntity {

    @Id
    @Column(name = "CLIENT_ID", nullable = false, updatable = false)
    private long clientId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false, updatable = false)
    private ClientEntity client;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "MODIFIED_AT", insertable = false)
    @UpdateTimestamp
    private Instant modifiedAt;

    @Column(name = "AP_TITLE", nullable = false, length = 255)
    private String apTitle;

    @Column(name = "AP_CODE", nullable = false, length = 255)
    private String apCode;

    @Column(name = "AP_CALLBACK_URL", nullable = false, length = 255)
    private String apCallbackUrl;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "COMMUNICATION_CERTIFICATE")
    private byte[] communicationCertificate;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "APPLICATION_CERTIFICATE")
    private byte[] applicationCertificate;

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
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

    public String getApTitle() {
        return apTitle;
    }

    public void setApTitle(String apTitle) {
        this.apTitle = apTitle;
    }

    public String getApCode() {
        return apCode;
    }

    public void setApCode(String apCode) {
        this.apCode = apCode;
    }

    public String getApCallbackUrl() {
        return apCallbackUrl;
    }

    public void setApCallbackUrl(String apCallbackUrl) {
        this.apCallbackUrl = apCallbackUrl;
    }

    public byte[] getCommunicationCertificate() {
        return communicationCertificate;
    }

    public void setCommunicationCertificate(byte[] communicationCertificate) {
        this.communicationCertificate = communicationCertificate;
    }

    public byte[] getApplicationCertificate() {
        return applicationCertificate;
    }

    public void setApplicationCertificate(byte[] applicationCertificate) {
        this.applicationCertificate = applicationCertificate;
    }
}
