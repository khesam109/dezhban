package com.khesam.dezhban.dataaccess.local.entity;

import com.khesam.dezhban.common.AuthenticationActorType;
import com.khesam.dezhban.common.AuthenticationOutcome;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "LOGIN_EVENT", schema = "DEZHBAN")
public class LoginEventEntity {

    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(
            name = "loginEventSequenceGenerator",
            sequenceName = "LOGIN_EVENT_SEQ",
            allocationSize = 1,
            schema = "DEZHBAN"
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loginEventSequenceGenerator")
    private long id;

    @Column(name = "OCCURRED_AT", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTOR_TYPE", nullable = false, updatable = false, length = 20)
    private AuthenticationActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "OUTCOME", nullable = false, updatable = false, length = 20)
    private AuthenticationOutcome outcome;

    @Column(name = "PRESENTED_IDENTIFIER", nullable = false, updatable = false, length = 255)
    private String presentedIdentifier;

    @Column(name = "END_USER_ID", updatable = false)
    private Long endUserId;

    @Column(name = "CLIENT_ID", updatable = false)
    private Long clientId;

    @Column(name = "AUTHENTICATION_METHOD", nullable = false, updatable = false, length = 100)
    private String authenticationMethod;

    @Column(name = "FAILURE_REASON", updatable = false, length = 100)
    private String failureReason;

    @Column(name = "REMOTE_ADDRESS", updatable = false, length = 45)
    private String remoteAddress;

    @Column(name = "SESSION_ID", updatable = false, length = 100)
    private String sessionId;

    public long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public AuthenticationActorType getActorType() {
        return actorType;
    }

    public void setActorType(AuthenticationActorType actorType) {
        this.actorType = actorType;
    }

    public AuthenticationOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(AuthenticationOutcome outcome) {
        this.outcome = outcome;
    }

    public String getPresentedIdentifier() {
        return presentedIdentifier;
    }

    public void setPresentedIdentifier(String presentedIdentifier) {
        this.presentedIdentifier = presentedIdentifier;
    }

    public Long getEndUserId() {
        return endUserId;
    }

    public void setEndUserId(Long endUserId) {
        this.endUserId = endUserId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
