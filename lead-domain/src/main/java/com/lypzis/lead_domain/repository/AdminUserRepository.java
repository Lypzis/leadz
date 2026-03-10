package com.lypzis.lead_domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_domain.entity.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    boolean existsByEmail(String email);

    Optional<AdminUser> findByEmail(String email);
}
