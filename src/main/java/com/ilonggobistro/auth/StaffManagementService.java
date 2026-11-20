package com.ilonggobistro.auth;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import com.ilonggobistro.admin.StaffRole;
import com.ilonggobistro.shared.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StaffManagementService {

    private final AdminUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public StaffManagementService(AdminUserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Transactional(readOnly = true)
    public List<AdminUser> listAllStaff() {
        return userRepository.findAllByOrderByRoleAscFullNameAsc();
    }

    @Transactional(readOnly = true)
    public AdminUser getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found."));
    }

    @Transactional(readOnly = true)
    public AdminUser getByUsernameOrEmail(String principalName) {
        return userRepository.findByUsernameIgnoreCase(principalName)
                .or(() -> userRepository.findByEmailIgnoreCase(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found: " + principalName));
    }

    @Transactional
    public AdminUser registerStaff(StaffRegistrationForm form) {
        String username = form.getUsername().trim().toLowerCase(Locale.ENGLISH);
        String email = normalizeAndValidateGmail(form.getEmail());
        String phone = normalizePhone(form.getPhoneNumber());
        StaffRole role = StaffRole.valueOf(form.getRole().trim().toUpperCase(Locale.ENGLISH));

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username \"" + username + "\" is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Gmail address \"" + email + "\" is already registered to another staff member.");
        }

        AdminUser user = new AdminUser(
                username,
                passwordEncoder.encode(form.getInitialPassword()),
                form.getFullName().trim(),
                email,
                phone,
                role.name(),
                form.isMustChangePassword()
        );
        return userRepository.save(user);
    }

    @Transactional
    public AdminUser updateStaff(Long id, String fullName, String emailRaw, String phoneRaw, String roleRaw) {
        AdminUser user = getById(id);
        String email = normalizeAndValidateGmail(emailRaw);
        String phone = normalizePhone(phoneRaw);
        StaffRole targetRole = StaffRole.valueOf(roleRaw.trim().toUpperCase(Locale.ENGLISH));

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalArgumentException("Gmail address \"" + email + "\" belongs to another staff member.");
        }

        // Guard: never demote the last active ADMIN
        if ("ADMIN".equalsIgnoreCase(user.getRole()) && user.isEnabled() && targetRole != StaffRole.ADMIN) {
            if (userRepository.countByRoleAndEnabledTrue("ADMIN") <= 1) {
                throw new IllegalArgumentException("Cannot change role: at least one active ADMIN (Owner / Manager) must remain.");
            }
        }

        if (!email.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(email);
            user.setEmailVerified(false);
        }
        if (!phone.equals(user.getPhoneNumber())) {
            user.setPhoneNumber(phone);
            user.setPhoneVerified(false);
        }
        user.setFullName(fullName.trim());
        user.setRole(targetRole.name());
        return userRepository.save(user);
    }

    @Transactional
    public AdminUser toggleEnabled(Long id) {
        AdminUser user = getById(id);
        if (user.isEnabled() && "ADMIN".equalsIgnoreCase(user.getRole())) {
            if (userRepository.countByRoleAndEnabledTrue("ADMIN") <= 1) {
                throw new IllegalArgumentException("Cannot disable the last active ADMIN account.");
            }
        }
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    @Transactional
    public OtpService.OtpDispatchResult triggerAdminPasswordReset(Long userId, OtpChannel channel) {
        AdminUser user = getById(userId);
        user.setMustChangePassword(true);
        userRepository.save(user);
        return otpService.issueOtp(user, channel, OtpPurpose.PASSWORD_RESET);
    }

    @Transactional
    public AdminUser updateOwnContact(String principalName, String fullName, String emailRaw, String phoneRaw) {
        AdminUser user = getByUsernameOrEmail(principalName);
        String email = normalizeAndValidateGmail(emailRaw);
        String phone = normalizePhone(phoneRaw);

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new IllegalArgumentException("That Gmail address is already used by another staff account.");
        }
        if (!email.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(email);
            user.setEmailVerified(false);
        }
        if (!phone.equals(user.getPhoneNumber())) {
            user.setPhoneNumber(phone);
            user.setPhoneVerified(false);
        }
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        return userRepository.save(user);
    }

    @Transactional
    public OtpService.OtpDispatchResult sendOwnOtp(String principalName, OtpChannel channel, OtpPurpose purpose) {
        AdminUser user = getByUsernameOrEmail(principalName);
        return otpService.issueOtp(user, channel, purpose);
    }

    @Transactional
    public OtpChannel verifyOwnContactOtp(String principalName, String otpCode) {
        AdminUser user = getByUsernameOrEmail(principalName);
        return otpService.verifyAndConsume(user, OtpPurpose.ACCOUNT_VERIFY, otpCode);
    }

    @Transactional
    public void changeOwnPasswordWithOtp(String principalName, String otpCode, String newPassword, String confirmPassword) {
        validatePasswordPair(newPassword, confirmPassword);
        AdminUser user = getByUsernameOrEmail(principalName);
        otpService.verifyAndConsume(user, OtpPurpose.PASSWORD_CHANGE, otpCode);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public OtpService.OtpDispatchResult initiateForgotPassword(String identifier, OtpChannel channel) {
        AdminUser user = findEnabledByEmailOrPhone(identifier)
                .orElseThrow(() -> new IllegalArgumentException("No active staff account matched that Gmail address or WhatsApp number."));
        return otpService.issueOtp(user, channel, OtpPurpose.PASSWORD_RESET);
    }

    @Transactional
    public void completeForgotPassword(String identifier, String otpCode, String newPassword, String confirmPassword) {
        validatePasswordPair(newPassword, confirmPassword);
        AdminUser user = findEnabledByEmailOrPhone(identifier)
                .orElseThrow(() -> new IllegalArgumentException("No active staff account matched that Gmail address or WhatsApp number."));
        otpService.verifyAndConsume(user, OtpPurpose.PASSWORD_RESET, otpCode);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<AdminUser> findEnabledByEmailOrPhone(String rawIdentifier) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            return Optional.empty();
        }
        String trimmed = rawIdentifier.trim();
        Optional<AdminUser> byEmail = userRepository.findByEmailIgnoreCase(trimmed)
                .filter(AdminUser::isEnabled);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        String targetDigits = AdminUser.normalizePhilippineWhatsAppDigits(trimmed);
        if (targetDigits.length() < 10) {
            return Optional.empty();
        }
        return userRepository.findAll().stream()
                .filter(AdminUser::isEnabled)
                .filter(u -> targetDigits.equals(u.whatsappDigits()))
                .findFirst();
    }

    private void validatePasswordPair(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
    }

    private String normalizeAndValidateGmail(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("A Gmail address is required.");
        }
        String email = raw.trim().toLowerCase(Locale.ENGLISH);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (!email.endsWith("@gmail.com") && !email.endsWith("@googlemail.com")) {
            throw new IllegalArgumentException("Staff accounts must use a valid @gmail.com address for Gmail OTP & Google Sign-In.");
        }
        return email;
    }

    private String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("A Philippine mobile / WhatsApp number is required.");
        }
        String digits = AdminUser.normalizePhilippineWhatsAppDigits(raw);
        if (digits.length() < 10 || digits.length() > 15) {
            throw new IllegalArgumentException("Please enter a valid mobile / WhatsApp number (e.g., +63 917 123 4567 or 0917 123 4567).");
        }
        return raw.trim();
    }
}
