package com.khesam.dezhban.dataaccess.local.repository;

import com.khesam.dezhban.dataaccess.local.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
}
