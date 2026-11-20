package com.ilonggobistro.auth;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.StaffRole;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final StaffManagementService staffService;
    private final AuthNotificationProperties properties;

    public AdminStaffController(StaffManagementService staffService,
                                AuthNotificationProperties properties) {
        this.staffService = staffService;
        this.properties = properties;
    }

    @GetMapping
    public String staffDirectory(Model model) {
        model.addAttribute("staffList", staffService.listAllStaff());
        model.addAttribute("roles", StaffRole.values());
        model.addAttribute("mailConfigured", properties.isMailSmtpConfigured());
        model.addAttribute("whatsappConfigured", properties.isWhatsAppMetaConfigured());
        if (!model.containsAttribute("staffForm")) {
            model.addAttribute("staffForm", new StaffRegistrationForm());
        }
        return "admin/staff";
    }

    @PostMapping
    public String registerStaff(
            @Valid @ModelAttribute("staffForm") StaffRegistrationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("staffList", staffService.listAllStaff());
            model.addAttribute("roles", StaffRole.values());
            model.addAttribute("mailConfigured", properties.isMailSmtpConfigured());
            model.addAttribute("whatsappConfigured", properties.isWhatsAppMetaConfigured());
            return "admin/staff";
        }
        try {
            AdminUser created = staffService.registerStaff(form);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Registered " + created.getFullName() + " (" + created.roleLabel() + ") with Gmail " + created.getEmail() + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/{id}/update")
    public String updateStaff(
            @PathVariable Long id,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("role") String role,
            RedirectAttributes redirectAttributes) {
        try {
            AdminUser updated = staffService.updateStaff(id, fullName, email, phoneNumber, role);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Updated " + updated.getFullName() + " (" + updated.roleLabel() + ").");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            AdminUser updated = staffService.toggleEnabled(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    updated.getFullName() + " is now " + (updated.isEnabled() ? "Enabled" : "Disabled") + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/{id}/reset-password")
    public String triggerPasswordReset(
            @PathVariable Long id,
            @RequestParam(name = "channel", defaultValue = "GMAIL") OtpChannel channel,
            RedirectAttributes redirectAttributes) {
        try {
            OtpService.OtpDispatchResult result = staffService.triggerAdminPasswordReset(id, channel);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Password reset OTP sent via " + result.providerLabel() + " to " + result.maskedTarget()
                            + " (expires in " + result.expiryMinutes() + " min).");
            if (result.devPreviewCode() != null) {
                redirectAttributes.addFlashAttribute("devOtpPreview",
                        "DEV SIMULATOR (" + result.channel().getLabel() + "): Reset OTP Code = " + result.devPreviewCode());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/staff";
    }
}
