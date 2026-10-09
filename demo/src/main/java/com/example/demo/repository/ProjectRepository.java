package com.example.demo.repository;

import com.example.demo.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Page<Project> findAllByCreatedById(UUID userId, Pageable pageable);

    Project findProjectByName(String name);
}
