package com.khesam.dezhban.dataaccess.local.entity;

import com.khesam.dezhban.common.ClientType;
import com.khesam.dezhban.common.GrantType;
import com.khesam.dezhban.common.ClientAuthenticationType;
import com.khesam.dezhban.dataaccess.local.converter.ClientAuthenticationTypeConverter;
import com.khesam.dezhban.dataaccess.local.converter.GrantTypeConverter;
import com.khesam.dezhban.dataaccess.local.converter.RedirectUriConverter;
import com.khesam.dezhban.dataaccess.local.converter.ScopeConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "CLIENT", schema = "DEZHBAN")
public class ClientEntity {

    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(name = "clientSequenceGenerator", sequenceName = "CLIENT_SEQ", allocationSize = 1, schema = "DEZHBAN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clientSequenceGenerator")
    private long id;

    @Column(name = "CLIENT_ID", nullable = false)
    private String clientId;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;

    @Column(name = "PUBLIC_CLIENT", nullable = false)
    private boolean publicClient;

    @Column(name = "SECRET_HASH", nullable = false)
    private String secretHash;

    @Column(name = "SECRET_EXPIRES_AT")
    private Instant secretHashExpiresAt;

    @Column(name = "CLIENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ClientType clientType;

    @Column(name = "AUTHENTICATION_METHODS", nullable = false)
    @Convert(converter = ClientAuthenticationTypeConverter.class)
    private Set<ClientAuthenticationType> authenticationMethods = new HashSet<>();

    @Column(name = "AUTHORIZATION_GRANT_TYPES", nullable = false)
    @Convert(converter = GrantTypeConverter.class)
    private Set<GrantType> grantTypes = new HashSet<>();

    @Column(name = "REDIRECT_URIS", nullable = false)
    @Convert(converter = RedirectUriConverter.class)
    private Set<String> redirectUris = new HashSet<>();

    @Column(name = "POST_LOGOUT_REDIRECT_URI")
    private String postLogoutRedirectUri;

    @Column(name = "SCOPES", nullable = false)
    @Convert(converter = ScopeConverter.class)
    private Set<String> scopes = new HashSet<>();

    @Column(name = "CLIENT_SETTINGS", nullable = false)
    private String clientSettings;

    @Column(name = "TOKEN_SETTINGS", nullable = false)
    private String tokenSettings;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "MODIFIED_AT", insertable = false)
    @UpdateTimestamp
    private Instant modifiedAt;

    @Column(name = "NOT_BEFORE")
    private Instant notBefore;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }

    public Instant getSecretHashExpiresAt() {
        return secretHashExpiresAt;
    }

    public void setSecretHashExpiresAt(Instant secretHashExpiresAt) {
        this.secretHashExpiresAt = secretHashExpiresAt;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public Set<ClientAuthenticationType> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(Set<ClientAuthenticationType> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public Set<GrantType> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(Set<GrantType> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(String clientSettings) {
        this.clientSettings = clientSettings;
    }

    public String getTokenSettings() {
        return tokenSettings;
    }

    public void setTokenSettings(String tokenSettings) {
        this.tokenSettings = tokenSettings;
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
}
