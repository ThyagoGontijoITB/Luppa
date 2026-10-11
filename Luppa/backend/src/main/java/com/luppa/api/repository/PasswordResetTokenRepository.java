package com.luppa.api.repository;

import com.luppa.api.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findFirstByTokenHash(byte[] tokenHash);

    @Modifying
    @Query("""
            update PasswordResetToken token
            set token.consumedAt = :consumedAt
            where token.user.id = :userId
              and token.consumedAt is null
            """)
    int consumeOpenTokensForUser(@Param("userId") Integer userId, @Param("consumedAt") LocalDateTime consumedAt);
}
