package com.ilonggobistro.event;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/events")
public class EventController {

    private final BusinessEventService eventService;

    public EventController(BusinessEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public String eventsHub(
            @RequestParam(name = "type", required = false, defaultValue = "All") String eventType,
            Model model) {
        populateHubModel(eventType, model);
        return "pages/events";
    }

    @GetMapping("/fragment")
    public String eventsFragment(
            @RequestParam(name = "type", required = false, defaultValue = "All") String eventType,
            Model model) {
        populateHubModel(eventType, model);
        return "fragments/event-cards :: grid";
    }

    @GetMapping("/{slug}")
    public String eventAlbum(@PathVariable String slug, Model model) {
        BusinessEvent event = eventService.getPublishedBySlug(slug);
        model.addAttribute("event", event);
        model.addAttribute("approvedPhotos", event.approvedPhotos());
        model.addAttribute("maxBatch", BusinessEventService.MAX_FILES_PER_BATCH);
        return "pages/event-detail";
    }

    @PostMapping("/{slug}/upload")
    public String uploadGuestPhotos(
            @PathVariable String slug,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "guestName", required = false) String guestName,
            @RequestParam(name = "caption", required = false) String caption,
            RedirectAttributes redirectAttributes) {
        try {
            int count = eventService.uploadGuestPhotos(slug, files, guestName, caption);
            redirectAttributes.addFlashAttribute("uploadSuccess",
                    "Salamat! Uploaded " + count + " photo" + (count == 1 ? "" : "s")
                            + ". Your shots are now queued for quick staff approval and will appear in this album shortly.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("uploadError", ex.getMessage());
        }
        return "redirect:/events/" + slug + "#guest-upload";
    }

    private void populateHubModel(String eventType, Model model) {
        model.addAttribute("eventTypes", BusinessEventService.EVENT_TYPES);
        model.addAttribute("activeType", eventType == null || eventType.isBlank() ? "All" : eventType);
        model.addAttribute("events", eventService.listPublished(eventType));
    }
}
