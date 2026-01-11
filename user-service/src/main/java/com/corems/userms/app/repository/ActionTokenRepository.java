package com.corems.userms.app.repository;

import com.corems.userms.app.entity.ActionTokenEntity;
import com.corems.userms.app.model.enums.UserActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionTokenRepository extends JpaRepository<ActionTokenEntity, Long> {

    Optional<ActionTokenEntity> findByTokenHashAndUsedFalse(String tokenHash);

    Optional<ActionTokenEntity> findByTokenHashAndActionTypeAndUsedFalse(String tokenHash, UserActionType actionType);

    @Modifying
    @Query("DELETE FROM ActionTokenEntity a WHERE a.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM ActionTokenEntity a WHERE a.user.uuid = :userUuid AND a.actionType = :actionType")
    void deleteByUserUuidAndActionType(@Param("userUuid") UUID userUuid, @Param("actionType") UserActionType actionType);
}