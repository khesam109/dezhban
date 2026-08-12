package com.khesam.dezhban.dataaccess.local.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "OAUTH2_AUTHORIZATION", schema = "DEZHBAN")
public class OAuth2AuthorizationEntity {

    @Id
    @Column(name = "ID", nullable = false, length = 100)
    private String id;

    @Column(name = "REGISTERED_CLIENT_ID", nullable = false, length = 100)
    private String registeredClientId;

    @Column(name = "PRINCIPAL_NAME", nullable = false, length = 200)
    private String principalName;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "AUTHORIZATION_DATA", nullable = false)
    private byte[] authorizationData;

    @Column(name = "STATE_DIGEST", length = 64)
    private String stateDigest;

    @Column(name = "AUTHORIZATION_CODE_DIGEST", length = 64)
    private String authorizationCodeDigest;

    @Column(name = "ACCESS_TOKEN_DIGEST", length = 64)
    private String accessTokenDigest;

    @Column(name = "OIDC_ID_TOKEN_DIGEST", length = 64)
    private String oidcIdTokenDigest;

    @Column(name = "REFRESH_TOKEN_DIGEST", length = 64)
    private String refreshTokenDigest;

    @Column(name = "USER_CODE_DIGEST", length = 64)
    private String userCodeDigest;

    @Column(name = "DEVICE_CODE_DIGEST", length = 64)
    private String deviceCodeDigest;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegisteredClientId() {
        return registeredClientId;
    }

    public void setRegisteredClientId(String registeredClientId) {
        this.registeredClientId = registeredClientId;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public byte[] getAuthorizationData() {
        return authorizationData;
    }

    public void setAuthorizationData(byte[] authorizationData) {
        this.authorizationData = authorizationData;
    }

    public String getStateDigest() {
        return stateDigest;
    }

    public void setStateDigest(String stateDigest) {
        this.stateDigest = stateDigest;
    }

    public String getAuthorizationCodeDigest() {
        return authorizationCodeDigest;
    }

    public void setAuthorizationCodeDigest(String authorizationCodeDigest) {
        this.authorizationCodeDigest = authorizationCodeDigest;
    }

    public String getAccessTokenDigest() {
        return accessTokenDigest;
    }

    public void setAccessTokenDigest(String accessTokenDigest) {
        this.accessTokenDigest = accessTokenDigest;
    }

    public String getOidcIdTokenDigest() {
        return oidcIdTokenDigest;
    }

    public void setOidcIdTokenDigest(String oidcIdTokenDigest) {
        this.oidcIdTokenDigest = oidcIdTokenDigest;
    }

    public String getRefreshTokenDigest() {
        return refreshTokenDigest;
    }

    public void setRefreshTokenDigest(String refreshTokenDigest) {
        this.refreshTokenDigest = refreshTokenDigest;
    }

    public String getUserCodeDigest() {
        return userCodeDigest;
    }

    public void setUserCodeDigest(String userCodeDigest) {
        this.userCodeDigest = userCodeDigest;
    }

    public String getDeviceCodeDigest() {
        return deviceCodeDigest;
    }

    public void setDeviceCodeDigest(String deviceCodeDigest) {
        this.deviceCodeDigest = deviceCodeDigest;
    }

    public long getVersion() {
        return version;
    }
}
