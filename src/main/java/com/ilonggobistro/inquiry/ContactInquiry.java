package com.ilonggobistro.inquiry;

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
@Table(name = "contact_inquiries")
public class ContactInquiry {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "inquiry_type", nullable = false, length = 50)
    private String inquiryType = "General";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ContactInquiry() {
    }

    public ContactInquiry(String name, String phone, String email, String inquiryType, String message) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.inquiryType = inquiryType;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String formattedCreatedAt() {
        return createdAt != null ? createdAt.format(FMT) : "";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getInquiryType() {
        return inquiryType;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
