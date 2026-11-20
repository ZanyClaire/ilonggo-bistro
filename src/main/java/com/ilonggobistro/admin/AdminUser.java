package com.ilonggobistro.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "admin_users")
public class AdminUser {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 30)
    private String role = "ADMIN";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminUser() {
    }

    public AdminUser(String username, String passwordHash, String fullName,
                     String email, String phoneNumber, String role, boolean mustChangePassword) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.enabled = true;
        this.mustChangePassword = mustChangePassword;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public String whatsappDigits() {
        return normalizePhilippineWhatsAppDigits(this.phoneNumber);
    }

    public String whatsappChatUrl() {
        String digits = whatsappDigits();
        return digits.isBlank() ? "#" : "https://wa.me/" + digits;
    }

    public String roleLabel() {
        return StaffRole.fromStringOrDefault(role).getLabel();
    }

    public String formattedCreatedAt() {
        return createdAt != null ? createdAt.format(DATE_FMT) : "";
    }

    public static String normalizePhilippineWhatsAppDigits(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.startsWith("09") && digits.length() == 11) {
            return "63" + digits.substring(1);
        }
        if (digits.startsWith("9") && digits.length() == 10) {
            return "63" + digits;
        }
        return digits;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
