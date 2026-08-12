package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.AuthorizationRoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuthorizationRoleRepository extends JpaRepository<AuthorizationRoleEntity, Long> {

    @EntityGraph(attributePaths = "scopes")
    Optional<AuthorizationRoleEntity> findByCode(String code);

    @EntityGraph(attributePaths = "scopes")
    List<AuthorizationRoleEntity> findAllByCodeIn(Collection<String> codes);

    @EntityGraph(attributePaths = "scopes")
    List<AuthorizationRoleEntity> findAllByOrderByCodeAsc();
}
