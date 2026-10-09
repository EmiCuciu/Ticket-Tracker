package com.example.demo.repository;

import com.example.demo.domain.AuthProvider;
import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByFullName(String fullName);

    Page<User> findAllByRole(Role role, Pageable pageable);

    Page<User> findAllByPasswordIsNull(Pageable pageable);

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmailAndAuthProvider(String email, AuthProvider authProvider);
}