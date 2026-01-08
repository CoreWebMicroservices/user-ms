package com.corems.userms.app.repository;

import com.corems.userms.app.entity.VerificationTokenEntity;
import com.corems.userms.app.entity.VerificationTokenEntity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {

    Optional<VerificationTokenEntity> findByTokenAndTypeAndIsUsedFalse(String token, VerificationType type);

    List<VerificationTokenEntity> findByUserUuidAndTypeAndIsUsedFalse(UUID userUuid, VerificationType type);

    List<VerificationTokenEntity> findByExpiresAtBefore(LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);
}