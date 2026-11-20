package com.ilonggobistro.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthOtpTokenRepository extends JpaRepository<AuthOtpToken, Long> {

    List<AuthOtpToken> findByUserIdAndPurposeAndUsedFalse(Long userId, OtpPurpose purpose);

    Optional<AuthOtpToken> findFirstByUserIdAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId, OtpPurpose purpose, LocalDateTime now
    );
}
