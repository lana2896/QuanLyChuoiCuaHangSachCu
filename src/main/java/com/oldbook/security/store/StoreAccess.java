package com.oldbook.security.store;

import com.oldbook.constant.auth.Role;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class StoreAccess {

    public AuthenticatedUserDetails requireModerator() {
        AuthenticatedUserDetails actor = currentActor();
        requireRole(Role.QUAN_LY, Role.QUAN_TRI_VIEN);
        return actor;
    }

    private AuthenticatedUserDetails currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Integer maND)
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails actor)
                || maND <= 0 || actor.maTK() == null || actor.maTK() <= 0
                || !maND.equals(actor.maND())) {
            throw new AuthenticationCredentialsNotFoundException("Vui lòng đăng nhập bằng tài khoản hợp lệ");
        }
        return actor;
    }

    private void requireRole(Role... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        for (Role role : roles) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> role.getAuthority().equals(authority.getAuthority()))) {
                return;
            }
        }
        throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
    }
}
