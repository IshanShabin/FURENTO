package com.furniture.rental.repository;

import com.furniture.rental.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    @Transactional
    long deleteByExpiresAtBefore(Instant cutoff);
}