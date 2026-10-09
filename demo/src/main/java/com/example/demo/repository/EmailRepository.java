package com.example.demo.repository;

import com.example.demo.domain.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface EmailRepository extends JpaRepository<Email, UUID> {
    boolean existsByDedupeKey(String dedupeKey);

    @Modifying
    @Transactional
    @Query("DELETE FROM Email e WHERE e.status <> 'PENDING' AND e.createdAt < :threshold")
    int deleteFinishedBefore(@Param("threshold") Instant threshold);

}