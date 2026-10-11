package com.luppa.api.repository;

import com.luppa.api.entity.PendingRegistration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingRegistration> findByEmail(String email);
}
