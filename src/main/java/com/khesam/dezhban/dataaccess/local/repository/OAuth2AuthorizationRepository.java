package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.OAuth2AuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OAuth2AuthorizationRepository
        extends JpaRepository<OAuth2AuthorizationEntity, String> {

    Optional<OAuth2AuthorizationEntity> findByStateDigest(String digest);

    Optional<OAuth2AuthorizationEntity> findByAuthorizationCodeDigest(String digest);

    Optional<OAuth2AuthorizationEntity> findByAccessTokenDigest(String digest);

    Optional<OAuth2AuthorizationEntity> findByOidcIdTokenDigest(String digest);

    Optional<OAuth2AuthorizationEntity> findByRefreshTokenDigest(String digest);

    Optional<OAuth2AuthorizationEntity> findByUserCodeDigest(String digest);

    Optional<OAuth2AuthorizationEntity> findByDeviceCodeDigest(String digest);

    void deleteAllByPrincipalName(String principalName);

    void deleteAllByRegisteredClientId(String registeredClientId);

    @Query("""
            select authorization
            from OAuth2AuthorizationEntity authorization
            where authorization.stateDigest = :digest
               or authorization.authorizationCodeDigest = :digest
               or authorization.accessTokenDigest = :digest
               or authorization.oidcIdTokenDigest = :digest
               or authorization.refreshTokenDigest = :digest
               or authorization.userCodeDigest = :digest
               or authorization.deviceCodeDigest = :digest
            """)
    List<OAuth2AuthorizationEntity> findAllByTokenDigest(@Param("digest") String digest);
}
