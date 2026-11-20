package com.ilonggobistro.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StaffRegistrationForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 120)
    private String fullName;

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,40}$", message = "Username must be 3–40 letters, digits, dots, underscores, or hyphens")
    private String username;

    @NotBlank(message = "Gmail address is required")
    @Email(message = "Enter a valid Gmail address")
    @Size(max = 160)
    private String email;

    @NotBlank(message = "Philippine mobile / WhatsApp number is required")
    @Pattern(regexp = "^[+0-9()\\s-]{10,25}$", message = "Enter a valid mobile number (e.g., +63 917 123 4567 or 0917 123 4567)")
    private String phoneNumber;

    @NotBlank(message = "Please choose a role")
    private String role = "CASHIER";

    @NotBlank(message = "Initial password is required")
    @Size(min = 8, max = 80, message = "Password must be at least 8 characters")
    private String initialPassword;

    private boolean mustChangePassword = true;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getInitialPassword() { return initialPassword; }
    public void setInitialPassword(String initialPassword) { this.initialPassword = initialPassword; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
