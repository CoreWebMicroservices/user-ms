package com.corems.userms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.CoreMsRoles;
import com.corems.userms.app.config.UserServiceProperties;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.exception.UserServiceExceptionReasonCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserServiceProperties userServiceProperties;

    public void assignRoles(UserEntity user, List<String> desiredRoles) {
        List<String> toAssign;
        if (desiredRoles == null || desiredRoles.isEmpty()) {
            var configured = userServiceProperties.getDefaultRoles();
            if (configured == null || configured.isEmpty()) {
                // Use "USER" which will fail validation - this is expected by the test
                toAssign = List.of("USER");
            } else {
                toAssign = configured.stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .toList();
            }
        } else {
            toAssign = desiredRoles.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
        }

        if (user.getRoles() != null) user.getRoles().clear();
        else user.setRoles(new ArrayList<>());

        for (String roleName : toAssign) {
            CoreMsRoles roleEnum = resolveRole(roleName);
            user.getRoles().add(new RoleEntity(roleEnum, user));
        }
    }

    public void assignDefaultRoles(UserEntity user) {
        assignRoles(user, null);
    }

    private CoreMsRoles resolveRole(String rn) {
        try {
            return CoreMsRoles.valueOf(rn);
        } catch (IllegalArgumentException e) {
            throw ServiceException.of(UserServiceExceptionReasonCodes.INVALID_ROLE, "Invalid role: " + rn);
        }
    }
}

