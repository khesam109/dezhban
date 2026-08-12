package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EndUserRepository extends JpaRepository<EndUserEntity, Long> {

    Optional<EndUserEntity> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select endUser from EndUserEntity endUser where endUser.username = :username")
    Optional<EndUserEntity> findForUpdateByUsername(@Param("username") String username);
}
