package com.ilonggobistro.event;

import com.ilonggobistro.shared.ImageStorageService;
import com.ilonggobistro.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Service
public class BusinessEventService {

    public static final int MAX_FILES_PER_BATCH = 20;

    public static final List<String> EVENT_TYPES = List.of(
            "Awards Night",
            "Dining Events",
            "Catering",
            "Family Celebration",
            "Community Night"
    );

    private final BusinessEventRepository eventRepository;
    private final EventPhotoRepository photoRepository;
    private final ImageStorageService imageStorageService;

    public BusinessEventService(BusinessEventRepository eventRepository,
                                EventPhotoRepository photoRepository,
                                ImageStorageService imageStorageService) {
        this.eventRepository = eventRepository;
        this.photoRepository = photoRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public List<BusinessEvent> listPublished(String eventType) {
        if (eventType == null || eventType.isBlank() || "All".equalsIgnoreCase(eventType)) {
            return eventRepository.findByPublishedTrueOrderByEventDateDescIdDesc();
        }
        return eventRepository.findByPublishedTrueAndEventTypeIgnoreCaseOrderByEventDateDescIdDesc(eventType.trim());
    }

    @Transactional(readOnly = true)
    public List<BusinessEvent> listAll() {
        return eventRepository.findAllByOrderByEventDateDescIdDesc();
    }

    @Transactional(readOnly = true)
    public BusinessEvent getPublishedBySlug(String slug) {
        return eventRepository.findWithPhotosBySlugIgnoreCaseAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Event album not found."));
    }

    @Transactional(readOnly = true)
    public BusinessEvent getById(Long id) {
        return eventRepository.findWithPhotosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business event not found."));
    }

    @Transactional
    public BusinessEvent createEvent(BusinessEventForm form, List<MultipartFile> initialFiles, String adminName) {
        String slug = uniqueSlug(form.getTitle());
        BusinessEvent event = new BusinessEvent(
                form.getTitle().trim(),
                slug,
                form.getEventType().trim(),
                form.getEventDate(),
                form.getLocation().trim(),
                form.getDescription() != null ? form.getDescription().trim() : "",
                null,
                form.isAllowGuestUploads(),
                form.isPublished()
        );
        BusinessEvent saved = eventRepository.save(event);

        List<MultipartFile> nonEmpty = filterNonEmpty(initialFiles);
        if (!nonEmpty.isEmpty()) {
            uploadPhotosToEvent(saved, nonEmpty, "", adminName, true);
        }
        return saved;
    }

    @Transactional
    public BusinessEvent updateEvent(Long id, BusinessEventForm form) {
        BusinessEvent event = getById(id);
        event.setTitle(form.getTitle().trim());
        event.setEventType(form.getEventType().trim());
        event.setEventDate(form.getEventDate());
        event.setLocation(form.getLocation().trim());
        event.setDescription(form.getDescription() != null ? form.getDescription().trim() : "");
        event.setAllowGuestUploads(form.isAllowGuestUploads());
        event.setPublished(form.isPublished());
        return eventRepository.save(event);
    }

    @Transactional
    public int uploadAdminPhotos(Long eventId, List<MultipartFile> files, String caption, String adminName) {
        BusinessEvent event = getById(eventId);
        return uploadPhotosToEvent(event, filterNonEmpty(files), caption, adminName, true);
    }

    @Transactional
    public int uploadGuestPhotos(String slug, List<MultipartFile> files, String guestName, String caption) {
        BusinessEvent event = getPublishedBySlug(slug);
        if (!event.isAllowGuestUploads()) {
            throw new IllegalArgumentException("Guest photo uploads are currently closed for this event.");
        }
        String uploader = (guestName == null || guestName.isBlank()) ? "Bistro Guest" : guestName.trim();
        return uploadPhotosToEvent(event, filterNonEmpty(files), caption, uploader, false);
    }

    @Transactional
    public void approvePhoto(Long eventId, Long photoId) {
        BusinessEvent event = getById(eventId);
        EventPhoto photo = photoRepository.findByIdAndEventId(photoId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event photo not found."));
        photo.setApproved(true);
        if (event.getCoverImageUrl() == null || event.getCoverImageUrl().isBlank()) {
            event.setCoverImageUrl(photo.getImageUrl());
            eventRepository.save(event);
        }
        photoRepository.save(photo);
    }

    @Transactional
    public void setCoverPhoto(Long eventId, Long photoId) {
        BusinessEvent event = getById(eventId);
        EventPhoto photo = photoRepository.findByIdAndEventId(photoId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event photo not found."));
        photo.setApproved(true);
        event.setCoverImageUrl(photo.getImageUrl());
        eventRepository.save(event);
    }

    @Transactional
    public void deletePhoto(Long eventId, Long photoId) {
        BusinessEvent event = getById(eventId);
        EventPhoto photo = photoRepository.findByIdAndEventId(photoId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event photo not found."));
        boolean wasCover = photo.getImageUrl().equals(event.getCoverImageUrl());
        event.getPhotos().remove(photo);
        photoRepository.delete(photo);
        if (wasCover) {
            String nextCover = event.approvedPhotos().stream()
                    .map(EventPhoto::getImageUrl)
                    .findFirst()
                    .orElse(null);
            event.setCoverImageUrl(nextCover);
            eventRepository.save(event);
        }
    }

    @Transactional
    public void deleteEvent(Long id) {
        BusinessEvent event = getById(id);
        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public long countPendingGuestPhotos() {
        return photoRepository.countByApprovedFalse();
    }

    private int uploadPhotosToEvent(BusinessEvent event, List<MultipartFile> files,
                                    String caption, String uploader, boolean isAdmin) {
        if (files.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one photo (JPG, PNG, or WEBP) to upload.");
        }
        if (files.size() > MAX_FILES_PER_BATCH) {
            throw new IllegalArgumentException("You can upload up to " + MAX_FILES_PER_BATCH + " photos per batch.");
        }

        String cleanCaption = caption != null ? caption.trim() : "";
        String role = isAdmin ? "ADMIN" : "GUEST";
        int nextOrder = event.getPhotos().size() + 1;
        int uploadedCount = 0;

        for (MultipartFile file : files) {
            String storedUrl = imageStorageService.storeImage(file);
            if (storedUrl != null) {
                EventPhoto photo = new EventPhoto(storedUrl, cleanCaption, uploader, role, isAdmin, nextOrder++);
                event.addPhoto(photo);
                if (isAdmin && (event.getCoverImageUrl() == null || event.getCoverImageUrl().isBlank())) {
                    event.setCoverImageUrl(storedUrl);
                }
                uploadedCount++;
            }
        }
        eventRepository.save(event);
        return uploadedCount;
    }

    private List<MultipartFile> filterNonEmpty(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    private String uniqueSlug(String title) {
        String base = title.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isBlank()) {
            base = "bistro-event";
        }
        String candidate = base;
        int suffix = 2;
        while (eventRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
