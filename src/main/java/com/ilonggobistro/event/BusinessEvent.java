package com.ilonggobistro.event;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "business_events")
public class BusinessEvent {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "allow_guest_uploads", nullable = false)
    private boolean allowGuestUploads = true;

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, uploadedAt DESC")
    private List<EventPhoto> photos = new ArrayList<>();

    protected BusinessEvent() {
    }

    public BusinessEvent(String title, String slug, String eventType, LocalDate eventDate,
                         String location, String description, String coverImageUrl,
                         boolean allowGuestUploads, boolean published) {
        this.title = title;
        this.slug = slug;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.location = location;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.allowGuestUploads = allowGuestUploads;
        this.published = published;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void addPhoto(EventPhoto photo) {
        photo.setEvent(this);
        this.photos.add(photo);
    }

    public List<EventPhoto> approvedPhotos() {
        return photos.stream().filter(EventPhoto::isApproved).toList();
    }

    public List<EventPhoto> pendingPhotos() {
        return photos.stream().filter(p -> !p.isApproved()).toList();
    }

    public int approvedPhotoCount() {
        return (int) photos.stream().filter(EventPhoto::isApproved).count();
    }

    public int pendingPhotoCount() {
        return (int) photos.stream().filter(p -> !p.isApproved()).count();
    }

    public String effectiveCoverUrl() {
        if (coverImageUrl != null && !coverImageUrl.isBlank()) {
            return coverImageUrl;
        }
        return approvedPhotos().stream()
                .map(EventPhoto::getImageUrl)
                .findFirst()
                .orElse("/images/vibe-evening.jpg");
    }

    public String formattedDate() {
        return eventDate != null ? eventDate.format(DATE_FMT) : "";
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public boolean isAllowGuestUploads() {
        return allowGuestUploads;
    }

    public void setAllowGuestUploads(boolean allowGuestUploads) {
        this.allowGuestUploads = allowGuestUploads;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<EventPhoto> getPhotos() {
        return photos;
    }
}
