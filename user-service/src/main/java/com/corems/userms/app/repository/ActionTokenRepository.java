package com.corems.userms.app.repository;

import com.corems.userms.app.entity.ActionTokenEntity;
import com.corems.userms.app.model.enums.UserActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionTokenRepository extends JpaRepository<ActionTokenEntity, Long> {

    Optional<ActionTokenEntity> findByTokenHashAndUsedFalse(String tokenHash);

    Optional<ActionTokenEntity> findByTokenHashAndActionTypeAndUsedFalse(String tokenHash, UserActionType actionType);
    
    Optional<ActionTokenEntity> findByTokenHashAndActionTypeAndUsedFalseAndUserUuid(String tokenHash, UserActionType actionType, UUID userUuid);

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByUserUuidAndActionType(UUID userUuid, UserActionType actionType);
}