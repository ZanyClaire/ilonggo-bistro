package com.ilonggobistro.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "event_photos")
public class EventPhoto {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private BusinessEvent event;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 255)
    private String caption;

    @Column(name = "uploaded_by", nullable = false, length = 120)
    private String uploadedBy;

    @Column(name = "uploader_role", nullable = false, length = 20)
    private String uploaderRole = "ADMIN";

    @Column(nullable = false)
    private boolean approved = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    protected EventPhoto() {
    }

    public EventPhoto(String imageUrl, String caption, String uploadedBy, String uploaderRole, boolean approved, int displayOrder) {
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.uploadedBy = uploadedBy;
        this.uploaderRole = uploaderRole;
        this.approved = approved;
        this.displayOrder = displayOrder;
    }

    @PrePersist
    void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    void setEvent(BusinessEvent event) {
        this.event = event;
    }

    public String formattedUploadedAt() {
        return uploadedAt != null ? uploadedAt.format(FMT) : "";
    }

    public Long getId() {
        return id;
    }

    public BusinessEvent getEvent() {
        return event;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getUploaderRole() {
        return uploaderRole;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
