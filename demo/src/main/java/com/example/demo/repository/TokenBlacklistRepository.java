package com.example.demo.repository;

import com.example.demo.domain.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);
}