package com.ilonggobistro.event;

import com.ilonggobistro.shared.BusinessHoursService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/events")
public class AdminEventController {

    private final BusinessEventService eventService;
    private final BusinessHoursService businessHoursService;

    public AdminEventController(BusinessEventService eventService,
                                BusinessHoursService businessHoursService) {
        this.eventService = eventService;
        this.businessHoursService = businessHoursService;
    }

    @GetMapping
    public String listEvents(Model model) {
        model.addAttribute("events", eventService.listAll());
        model.addAttribute("eventTypes", BusinessEventService.EVENT_TYPES);
        model.addAttribute("pendingCount", eventService.countPendingGuestPhotos());
        model.addAttribute("maxBatch", BusinessEventService.MAX_FILES_PER_BATCH);
        if (!model.containsAttribute("eventForm")) {
            BusinessEventForm form = new BusinessEventForm();
            form.setEventDate(businessHoursService.today());
            model.addAttribute("eventForm", form);
        }
        return "admin/events";
    }

    @PostMapping
    public String createEvent(
            @Valid @ModelAttribute("eventForm") BusinessEventForm form,
            BindingResult bindingResult,
            @RequestParam(name = "files", required = false) List<MultipartFile> files,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("events", eventService.listAll());
            model.addAttribute("eventTypes", BusinessEventService.EVENT_TYPES);
            model.addAttribute("pendingCount", eventService.countPendingGuestPhotos());
            model.addAttribute("maxBatch", BusinessEventService.MAX_FILES_PER_BATCH);
            return "admin/events";
        }
        try {
            String staff = principal != null ? principal.getName() : "admin";
            BusinessEvent created = eventService.createEvent(form, files, staff);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Created event \"" + created.getTitle() + "\" with " + created.getPhotos().size() + " photo(s).");
            return "redirect:/admin/events/" + created.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/events";
        }
    }

    @GetMapping("/{id}")
    public String manageEvent(@PathVariable Long id, Model model) {
        BusinessEvent event = eventService.getById(id);
        model.addAttribute("event", event);
        model.addAttribute("eventForm", BusinessEventForm.fromEntity(event));
        model.addAttribute("eventTypes", BusinessEventService.EVENT_TYPES);
        model.addAttribute("approvedPhotos", event.approvedPhotos());
        model.addAttribute("pendingPhotos", event.pendingPhotos());
        model.addAttribute("maxBatch", BusinessEventService.MAX_FILES_PER_BATCH);
        return "admin/event-manage";
    }

    @PostMapping("/{id}")
    public String updateEvent(
            @PathVariable Long id,
            @Valid @ModelAttribute("eventForm") BusinessEventForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashError", "Please complete required event fields.");
            return "redirect:/admin/events/" + id;
        }
        eventService.updateEvent(id, form);
        redirectAttributes.addFlashAttribute("flashSuccess", "Event details updated.");
        return "redirect:/admin/events/" + id;
    }

    @PostMapping("/{id}/photos")
    public String uploadAdminPhotos(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "caption", required = false) String caption,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String staff = principal != null ? principal.getName() : "admin";
            int uploaded = eventService.uploadAdminPhotos(id, files, caption, staff);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Uploaded " + uploaded + " photo" + (uploaded == 1 ? "" : "s") + " to this event album.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/events/" + id;
    }

    @PostMapping("/{eventId}/photos/{photoId}/approve")
    public String approvePhoto(@PathVariable Long eventId, @PathVariable Long photoId,
                               RedirectAttributes redirectAttributes) {
        eventService.approvePhoto(eventId, photoId);
        redirectAttributes.addFlashAttribute("flashSuccess", "Guest photo approved and published to the event album.");
        return "redirect:/admin/events/" + eventId;
    }

    @PostMapping("/{eventId}/photos/{photoId}/cover")
    public String setCover(@PathVariable Long eventId, @PathVariable Long photoId,
                           RedirectAttributes redirectAttributes) {
        eventService.setCoverPhoto(eventId, photoId);
        redirectAttributes.addFlashAttribute("flashSuccess", "Event cover photo updated.");
        return "redirect:/admin/events/" + eventId;
    }

    @PostMapping("/{eventId}/photos/{photoId}/delete")
    public String deletePhoto(@PathVariable Long eventId, @PathVariable Long photoId,
                              RedirectAttributes redirectAttributes) {
        eventService.deletePhoto(eventId, photoId);
        redirectAttributes.addFlashAttribute("flashSuccess", "Photo removed from event.");
        return "redirect:/admin/events/" + eventId;
    }

    @PostMapping("/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.deleteEvent(id);
        redirectAttributes.addFlashAttribute("flashSuccess", "Business event and its album deleted.");
        return "redirect:/admin/events";
    }
}
