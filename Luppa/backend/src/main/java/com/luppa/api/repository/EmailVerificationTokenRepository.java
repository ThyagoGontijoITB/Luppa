package com.luppa.api.repository;

import com.luppa.api.entity.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationToken> findFirstByTokenHash(byte[] tokenHash);

    @Modifying
    @Query("""
            update EmailVerificationToken token
            set token.consumedAt = :consumedAt
            where token.user.id = :userId
              and token.consumedAt is null
            """)
    int consumeOpenTokensForUser(@Param("userId") Integer userId, @Param("consumedAt") LocalDateTime consumedAt);
}
