package com.ilonggobistro.admin;

import com.ilonggobistro.auth.AuthNotificationProperties;
import com.ilonggobistro.auth.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    private final AuthNotificationProperties notificationProperties;
    private final OtpService otpService;
    private final AdminUserRepository userRepository;

    public AdminAuthController(AuthNotificationProperties notificationProperties,
                               OtpService otpService,
                               AdminUserRepository userRepository) {
        this.notificationProperties = notificationProperties;
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("googleOAuthEnabled", notificationProperties.isGoogleOAuthConfigured());
        model.addAttribute("devGoogleSimEnabled", !notificationProperties.isGoogleOAuthConfigured() && otpService.isDevOrTestProfile());
        return "admin/login";
    }

    /**
     * Local Dev Simulator for "Sign in with Google" when GOOGLE_CLIENT_ID/SECRET are not set yet.
     * Enforces the exact same authorization policy as real OAuth2: only enabled registered Gmail accounts succeed.
     */
    @PostMapping("/dev-google-login")
    public String devSimulatedGoogleLogin(
            @RequestParam("gmail") String gmail,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        if (notificationProperties.isGoogleOAuthConfigured() || !otpService.isDevOrTestProfile()) {
            redirectAttributes.addFlashAttribute("googleError", "Simulated Google login is disabled in production.");
            return "redirect:/admin/login";
        }

        String cleanEmail = gmail != null ? gmail.trim().toLowerCase() : "";
        Optional<AdminUser> staffOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (staffOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("googleError",
                    "Google account \"" + cleanEmail + "\" is not registered in the Ilonggo Bistro Staff Directory.");
            return "redirect:/admin/login";
        }

        AdminUser staff = staffOpt.get();
        if (!staff.isEnabled()) {
            redirectAttributes.addFlashAttribute("googleError", "This staff account has been disabled by the Manager.");
            return "redirect:/admin/login";
        }

        if (!staff.isEmailVerified()) {
            staff.setEmailVerified(true);
            userRepository.save(staff);
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staff.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + staff.getRole()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        if (staff.isMustChangePassword()) {
            return "redirect:/admin/account?mustChange=1";
        }
        if ("KITCHEN_STAFF".equalsIgnoreCase(staff.getRole())) {
            return "redirect:/admin/kitchen";
        }
        return "redirect:/admin";
    }

    @GetMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied(HttpServletRequest request, Model model) {
        Object deniedPath = request.getAttribute("deniedPath");
        model.addAttribute("deniedPath", deniedPath != null ? deniedPath.toString() : request.getRequestURI());
        return "error/403";
    }
}
