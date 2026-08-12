package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

    Optional<ClientEntity> findByClientId(String clientId);

    boolean existsByClientId(String clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select client from ClientEntity client where client.clientId = :clientId")
    Optional<ClientEntity> findForUpdateByClientId(@Param("clientId") String clientId);
}
