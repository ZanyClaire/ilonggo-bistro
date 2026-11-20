package com.ilonggobistro.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class BusinessEventForm {

    @NotBlank(message = "Event title is required")
    @Size(max = 160)
    private String title;

    @NotBlank(message = "Please choose an event type")
    @Size(max = 60)
    private String eventType = "Dining Events";

    @NotNull(message = "Event date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate;

    @NotBlank(message = "Location is required")
    @Size(max = 200)
    private String location = "Ilonggo Bistro · Burgos-Osmeña St, Barotac Nuevo";

    @Size(max = 2000)
    private String description;

    private boolean allowGuestUploads = true;
    private boolean published = true;

    public BusinessEventForm() {
    }

    public static BusinessEventForm fromEntity(BusinessEvent event) {
        BusinessEventForm form = new BusinessEventForm();
        form.setTitle(event.getTitle());
        form.setEventType(event.getEventType());
        form.setEventDate(event.getEventDate());
        form.setLocation(event.getLocation());
        form.setDescription(event.getDescription());
        form.setAllowGuestUploads(event.isAllowGuestUploads());
        form.setPublished(event.isPublished());
        return form;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}
