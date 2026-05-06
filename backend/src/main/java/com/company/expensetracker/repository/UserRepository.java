package com.company.expensetracker.repository;

import com.company.expensetracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailHash(String emailHash);

    boolean existsByEmailHash(String emailHash);
}
