package com.ilonggobistro.auth;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class StaffLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AdminUserRepository userRepository;

    public StaffLoginSuccessHandler(AdminUserRepository userRepository) {
        this.userRepository = userRepository;
        setDefaultTargetUrl("/admin");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        String name = authentication.getName();
        Optional<AdminUser> staff = userRepository.findByUsernameIgnoreCase(name)
                .or(() -> userRepository.findByEmailIgnoreCase(name));

        if (staff.isPresent() && staff.get().isMustChangePassword()) {
            getRedirectStrategy().sendRedirect(request, response, "/admin/account?mustChange=1");
            return;
        }
        if (staff.isPresent() && "KITCHEN_STAFF".equalsIgnoreCase(staff.get().getRole())) {
            getRedirectStrategy().sendRedirect(request, response, "/admin/kitchen");
            return;
        }
        boolean isKitchenOnly = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_KITCHEN_STAFF".equals(a.getAuthority()))
                && authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isKitchenOnly) {
            getRedirectStrategy().sendRedirect(request, response, "/admin/kitchen");
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
