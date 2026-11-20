package com.ilonggobistro.auth;

import com.ilonggobistro.admin.AdminUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/account")
public class AdminAccountController {

    private final StaffManagementService staffService;
    private final AuthNotificationProperties properties;

    public AdminAccountController(StaffManagementService staffService,
                                  AuthNotificationProperties properties) {
        this.staffService = staffService;
        this.properties = properties;
    }

    @GetMapping
    public String accountSecurityPage(Principal principal, Model model) {
        AdminUser current = staffService.getByUsernameOrEmail(principal.getName());
        model.addAttribute("account", current);
        model.addAttribute("mailConfigured", properties.isMailSmtpConfigured());
        model.addAttribute("whatsappConfigured", properties.isWhatsAppMetaConfigured());
        return "admin/account";
    }

    @PostMapping("/contact")
    public String updateContact(
            Principal principal,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber,
            RedirectAttributes redirectAttributes) {
        try {
            staffService.updateOwnContact(principal.getName(), fullName, email, phoneNumber);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Account details updated. Re-verify any newly changed Gmail or WhatsApp number below.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/account";
    }

    @PostMapping("/verify/send")
    public String sendContactVerificationOtp(
            Principal principal,
            @RequestParam("channel") OtpChannel channel,
            RedirectAttributes redirectAttributes) {
        try {
            OtpService.OtpDispatchResult result = staffService.sendOwnOtp(principal.getName(), channel, OtpPurpose.ACCOUNT_VERIFY);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Verification OTP sent via " + result.providerLabel() + " to " + result.maskedTarget() + ".");
            if (result.devPreviewCode() != null) {
                redirectAttributes.addFlashAttribute("devOtpPreview",
                        "DEV SIMULATOR (" + result.channel().getLabel() + "): Contact Verification OTP = " + result.devPreviewCode());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/account";
    }

    @PostMapping("/verify/confirm")
    public String confirmContactVerificationOtp(
            Principal principal,
            @RequestParam("otpCode") String otpCode,
            RedirectAttributes redirectAttributes) {
        try {
            OtpChannel verifiedChannel = staffService.verifyOwnContactOtp(principal.getName(), otpCode);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Verified your " + verifiedChannel.getLabel() + " contact successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/account";
    }

    @PostMapping("/password/send-otp")
    public String sendPasswordChangeOtp(
            Principal principal,
            @RequestParam("channel") OtpChannel channel,
            RedirectAttributes redirectAttributes) {
        try {
            OtpService.OtpDispatchResult result = staffService.sendOwnOtp(principal.getName(), channel, OtpPurpose.PASSWORD_CHANGE);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "6-digit Password Change OTP sent via " + result.providerLabel() + " to " + result.maskedTarget()
                            + " (valid for " + result.expiryMinutes() + " min).");
            if (result.devPreviewCode() != null) {
                redirectAttributes.addFlashAttribute("devOtpPreview",
                        "DEV SIMULATOR (" + result.channel().getLabel() + "): Password Change OTP = " + result.devPreviewCode());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/account#change-password";
    }

    @PostMapping("/password/update")
    public String updatePasswordWithOtp(
            Principal principal,
            @RequestParam("otpCode") String otpCode,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            staffService.changeOwnPasswordWithOtp(principal.getName(), otpCode, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Password updated securely! Your new password is active immediately.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/account#change-password";
    }
}
