package com.ilonggobistro.shared;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalModelAttributes {

    private final StoreSettingsService storeSettingsService;
    private final BusinessHoursService businessHoursService;
    private final AdminUserRepository adminUserRepository;

    public GlobalModelAttributes(StoreSettingsService storeSettingsService,
                                 BusinessHoursService businessHoursService,
                                 AdminUserRepository adminUserRepository) {
        this.storeSettingsService = storeSettingsService;
        this.businessHoursService = businessHoursService;
        this.adminUserRepository = adminUserRepository;
    }

    @ModelAttribute("storeProfile")
    public StoreProfile storeProfile() {
        return storeSettingsService.getProfile();
    }

    @ModelAttribute("businessStatus")
    public BusinessHoursService.BusinessStatus businessStatus() {
        return businessHoursService.getCurrentStatus();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "/";
    }

    @ModelAttribute("currentStaff")
    public AdminUser currentStaff(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        return adminUserRepository.findByUsernameIgnoreCase(principal.getName())
                .or(() -> adminUserRepository.findByEmailIgnoreCase(principal.getName()))
                .orElse(null);
    }
}
