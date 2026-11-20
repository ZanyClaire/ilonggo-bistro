package com.ilonggobistro.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/forgot-password")
public class ForgotPasswordController {

    private final StaffManagementService staffService;

    public ForgotPasswordController(StaffManagementService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public String forgotPasswordPage(
            @RequestParam(name = "identifier", required = false) String identifier,
            Model model) {
        if (identifier != null && !identifier.isBlank() && !model.containsAttribute("resetIdentifier")) {
            model.addAttribute("resetIdentifier", identifier.trim());
        }
        return "admin/forgot-password";
    }

    @PostMapping("/request")
    public String requestOtp(
            @RequestParam("identifier") String identifier,
            @RequestParam(name = "channel", defaultValue = "GMAIL") OtpChannel channel,
            RedirectAttributes redirectAttributes) {
        try {
            OtpService.OtpDispatchResult result = staffService.initiateForgotPassword(identifier, channel);
            redirectAttributes.addFlashAttribute("resetIdentifier", identifier.trim());
            redirectAttributes.addFlashAttribute("otpSentMessage",
                    "6-digit code sent via " + result.providerLabel() + " to " + result.maskedTarget()
                            + " (expires in " + result.expiryMinutes() + " min).");
            if (result.devPreviewCode() != null) {
                redirectAttributes.addFlashAttribute("devOtpPreview",
                        "DEV SIMULATOR (" + result.channel().getLabel() + "): Password Reset OTP = " + result.devPreviewCode());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("resetIdentifier", identifier != null ? identifier.trim() : "");
            redirectAttributes.addFlashAttribute("resetError", ex.getMessage());
        }
        return "redirect:/admin/forgot-password";
    }

    @PostMapping("/confirm")
    public String confirmReset(
            @RequestParam("identifier") String identifier,
            @RequestParam("otpCode") String otpCode,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            staffService.completeForgotPassword(identifier, otpCode, newPassword, confirmPassword);
            return "redirect:/admin/login?resetSuccess";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("resetIdentifier", identifier != null ? identifier.trim() : "");
            redirectAttributes.addFlashAttribute("resetError", ex.getMessage());
            return "redirect:/admin/forgot-password";
        }
    }
}
