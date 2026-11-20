package com.ilonggobistro.auth;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffRbacAndOtpFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private StaffManagementService staffService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private StaffOidcUserService oidcUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(username = "cashier_test", roles = {"CASHIER"})
    void cashierHasPosDeliverySalesReadAccessButBlockedFromAdminAndVoid() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/pos")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/deliveries")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/sales")).andExpect(status().isOk());

        mockMvc.perform(get("/admin/staff")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/settings")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/menu")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reservations")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/sales/1/void").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "host_test", roles = {"BOOKING_HOST"})
    void bookingHostHasReservationsAndInquiriesOnly() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/reservations")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/inquiries")).andExpect(status().isOk());

        mockMvc.perform(get("/admin/pos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/menu")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/deliveries")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/staff")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "kitchen_test", roles = {"KITCHEN_STAFF"})
    void kitchenStaffHasMenuCategoriesAndViewOnlyDeliveries() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/menu")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/deliveries")).andExpect(status().isOk());

        mockMvc.perform(get("/admin/pos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/staff")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reservations")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/deliveries/1/dispatch").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanRegisterStaffAndCannotDisableLastAdmin() throws Exception {
        mockMvc.perform(post("/admin/staff")
                        .with(csrf())
                        .param("fullName", "Liza Bautista")
                        .param("username", "cashier.liza")
                        .param("email", "liza.bautista@gmail.com")
                        .param("phoneNumber", "0917 555 4321")
                        .param("role", "CASHIER")
                        .param("initialPassword", "TempPass123!")
                        .param("mustChangePassword", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/staff"));

        AdminUser saved = userRepository.findByUsernameIgnoreCase("cashier.liza").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("liza.bautista@gmail.com");
        assertThat(saved.whatsappChatUrl()).isEqualTo("https://wa.me/639175554321");
        assertThat(saved.isMustChangePassword()).isTrue();

        // Guard: cannot disable the sole active ADMIN
        AdminUser owner = userRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        assertThatThrownBy(() -> staffService.toggleEnabled(owner.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last active ADMIN");
    }

    @Test
    void forgotPasswordAndAccountPasswordChangeViaGmailAndWhatsAppOtp() throws Exception {
        // Ensure a dedicated test staff member exists
        if (!userRepository.existsByUsernameIgnoreCase("otp.tester")) {
            StaffRegistrationForm f = new StaffRegistrationForm();
            f.setFullName("OTP Tester");
            f.setUsername("otp.tester");
            f.setEmail("otp.tester@gmail.com");
            f.setPhoneNumber("+63 918 777 8899");
            f.setRole("CASHIER");
            f.setInitialPassword("InitialPass99!");
            f.setMustChangePassword(true);
            staffService.registerStaff(f);
        }

        // 1. Public Forgot Password via Gmail OTP
        mockMvc.perform(get("/admin/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reset Staff Password")));

        AdminUser user = userRepository.findByUsernameIgnoreCase("otp.tester").orElseThrow();
        OtpService.OtpDispatchResult resetResult = otpService.issueOtp(user, OtpChannel.GMAIL, OtpPurpose.PASSWORD_RESET);
        assertThat(resetResult.simulated()).isTrue();
        assertThat(resetResult.devPreviewCode()).matches("\\d{6}");

        mockMvc.perform(post("/admin/forgot-password/confirm")
                        .with(csrf())
                        .param("identifier", "otp.tester@gmail.com")
                        .param("otpCode", resetResult.devPreviewCode())
                        .param("newPassword", "FreshGmailPass2026!")
                        .param("confirmPassword", "FreshGmailPass2026!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?resetSuccess"));

        AdminUser afterReset = userRepository.findByUsernameIgnoreCase("otp.tester").orElseThrow();
        assertThat(passwordEncoder.matches("FreshGmailPass2026!", afterReset.getPasswordHash())).isTrue();
        assertThat(afterReset.isEmailVerified()).isTrue();
        assertThat(afterReset.isMustChangePassword()).isFalse();

        // Reusing the same OTP must fail
        assertThatThrownBy(() -> staffService.completeForgotPassword(
                "otp.tester@gmail.com", resetResult.devPreviewCode(), "AnotherPass1!", "AnotherPass1!"))
                .isInstanceOf(IllegalArgumentException.class);

        // 2. WhatsApp OTP for Password Change
        OtpService.OtpDispatchResult waResult = otpService.issueOtp(afterReset, OtpChannel.WHATSAPP, OtpPurpose.PASSWORD_CHANGE);
        assertThat(waResult.devPreviewCode()).matches("\\d{6}");

        staffService.changeOwnPasswordWithOtp("otp.tester", waResult.devPreviewCode(), "WhatsAppPass2026!", "WhatsAppPass2026!");
        AdminUser afterWa = userRepository.findByUsernameIgnoreCase("otp.tester").orElseThrow();
        assertThat(passwordEncoder.matches("WhatsAppPass2026!", afterWa.getPasswordHash())).isTrue();
        assertThat(afterWa.isPhoneVerified()).isTrue();
    }

    @Test
    void googleOidcAuthorizesRegisteredStaffAndRejectsUnknownGmail() throws Exception {
        OidcIdToken validToken = new OidcIdToken(
                "token-val", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("sub", "12345", "email", "ilonggobistro.admin@gmail.com", "email_verified", true)
        );
        OidcUser authorized = oidcUserService.authorizeRegisteredStaff(new DefaultOidcUser(List.of(), validToken));
        assertThat(authorized.getAuthorities()).anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        OidcIdToken unknownToken = new OidcIdToken(
                "token-unknown", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("sub", "99999", "email", "stranger.hacker@gmail.com", "email_verified", true)
        );
        assertThatThrownBy(() -> oidcUserService.authorizeRegisteredStaff(new DefaultOidcUser(List.of(), unknownToken)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("not registered");

        // Also test dev Google login simulator endpoint rejects unregistered Gmail
        mockMvc.perform(post("/admin/dev-google-login")
                        .with(csrf())
                        .param("gmail", "unknown.person@gmail.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"))
                .andExpect(flash().attributeExists("googleError"));

        // And authenticates registered Gmail
        MvcResult okRes = mockMvc.perform(post("/admin/dev-google-login")
                        .with(csrf())
                        .param("gmail", "ilonggobistro.admin@gmail.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();
        assertThat(okRes.getRequest().getSession(false)).isNotNull();
    }
}
