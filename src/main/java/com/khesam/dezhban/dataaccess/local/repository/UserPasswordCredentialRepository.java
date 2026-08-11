package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.UserPasswordCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPasswordCredentialRepository
        extends JpaRepository<UserPasswordCredentialEntity, Long> {

    @Query("""
            select credential
            from UserPasswordCredentialEntity credential
            join fetch credential.endUser endUser
            where endUser.username = :username
            """)
    Optional<UserPasswordCredentialEntity> findByUsername(@Param("username") String username);
}
