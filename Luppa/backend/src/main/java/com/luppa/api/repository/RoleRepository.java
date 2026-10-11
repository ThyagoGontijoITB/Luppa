package com.luppa.api.repository;

import com.luppa.api.entity.Role;
import com.luppa.api.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    default Optional<Role> findByName(UserRole name) {
        return findById(Role.idFor(name));
    }
}
