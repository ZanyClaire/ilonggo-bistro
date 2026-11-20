package com.ilonggobistro.admin;

import com.ilonggobistro.inquiry.InquiryService;
import com.ilonggobistro.shared.StoreSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminSettingsController {

    private final StoreSettingsService storeSettingsService;
    private final InquiryService inquiryService;

    public AdminSettingsController(StoreSettingsService storeSettingsService, InquiryService inquiryService) {
        this.storeSettingsService = storeSettingsService;
        this.inquiryService = inquiryService;
    }

    @GetMapping("/settings")
    public String settingsPage(Model model) {
        model.addAttribute("profile", storeSettingsService.getProfile());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(
            @RequestParam("bistroName") String bistroName,
            @RequestParam("tagline") String tagline,
            @RequestParam("addressLine") String addressLine,
            @RequestParam("plusCode") String plusCode,
            @RequestParam("openTime") String openTime,
            @RequestParam("closeTime") String closeTime,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("email") String email,
            @RequestParam("facebookUrl") String facebookUrl,
            @RequestParam("messengerUrl") String messengerUrl,
            @RequestParam("announcementBanner") String announcementBanner,
            @RequestParam("maxPartySize") String maxPartySize,
            @RequestParam("hourlySeatCapacity") String hourlySeatCapacity,
            RedirectAttributes redirectAttributes) {

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("bistro_name", bistroName);
        updates.put("tagline", tagline);
        updates.put("address_line", addressLine);
        updates.put("plus_code", plusCode);
        updates.put("open_time", openTime);
        updates.put("close_time", closeTime);
        updates.put("phone_number", phoneNumber);
        updates.put("email", email);
        updates.put("facebook_url", facebookUrl);
        updates.put("messenger_url", messengerUrl);
        updates.put("announcement_banner", announcementBanner);
        updates.put("max_party_size", maxPartySize);
        updates.put("hourly_seat_capacity", hourlySeatCapacity);

        storeSettingsService.updateSettings(updates);
        redirectAttributes.addFlashAttribute("flashSuccess", "Store profile & hours updated immediately.");
        return "redirect:/admin/settings";
    }

    @GetMapping("/inquiries")
    public String inquiriesPage(Model model) {
        model.addAttribute("inquiries", inquiryService.listAll());
        return "admin/inquiries";
    }

    @PostMapping("/inquiries/{id}/toggle-read")
    public String toggleRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        inquiryService.toggleRead(id);
        redirectAttributes.addFlashAttribute("flashSuccess", "Inquiry status updated.");
        return "redirect:/admin/inquiries";
    }
}
