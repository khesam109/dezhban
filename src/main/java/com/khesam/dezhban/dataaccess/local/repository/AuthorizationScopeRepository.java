package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.AuthorizationScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuthorizationScopeRepository extends JpaRepository<AuthorizationScopeEntity, Long> {

    Optional<AuthorizationScopeEntity> findByCode(String code);

    List<AuthorizationScopeEntity> findAllByCodeIn(Collection<String> codes);

    List<AuthorizationScopeEntity> findAllByOrderByCodeAsc();
}
