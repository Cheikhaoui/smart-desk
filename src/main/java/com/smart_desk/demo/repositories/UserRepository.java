package com.smart_desk.demo.repositories;

import com.smart_desk.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND u.active = :active
            ORDER BY u.fullName ASC
            """)
    List<User> search(@Param("role") User.Role role,
                      @Param("active") boolean active);
}
