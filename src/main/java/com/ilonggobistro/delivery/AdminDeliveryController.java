package com.ilonggobistro.delivery;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/deliveries")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    public AdminDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public String dispatchConsole(
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String statusFilter,
            Model model) {
        model.addAttribute("deliveries", deliveryService.listDeliveries(statusFilter));
        model.addAttribute("activeStatus", statusFilter.toUpperCase());
        model.addAttribute("statuses", DeliveryStatus.values());
        model.addAttribute("activeProvider", deliveryService.getProperties().resolvedProvider());
        model.addAttribute("hasLiveCredentials", deliveryService.getProperties().hasLiveApiCredentials());
        return "admin/deliveries";
    }

    @PostMapping("/{orderId}/dispatch")
    public String dispatchRider(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Delivery dispatched = deliveryService.bookRider(orderId);
            if (dispatched.getStatus() == DeliveryStatus.OUT_OF_COVERAGE) {
                redirectAttributes.addFlashAttribute("flashError",
                        "Third-party courier returned Out of Coverage for " + dispatched.getOrder().getReceiptNumber()
                                + ". Use the In-House Local Rider form below.");
            } else {
                redirectAttributes.addFlashAttribute("flashSuccess",
                        "Dispatched rider request via " + dispatched.getProvider().getLabel()
                                + " (ID: " + dispatched.getExternalDeliveryId() + ").");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", "Dispatch failed: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/{orderId}/assign-inhouse")
    public String assignInHouse(
            @PathVariable Long orderId,
            @RequestParam("riderName") String riderName,
            @RequestParam("riderPhone") String riderPhone,
            @RequestParam(name = "plateNumber", required = false) String plateNumber,
            RedirectAttributes redirectAttributes) {
        try {
            Delivery assigned = deliveryService.assignInHouseRider(orderId, riderName, riderPhone, plateNumber);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Assigned In-House Local Rider " + assigned.getRiderName() + " to " + assigned.getOrder().getReceiptNumber() + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/{deliveryId}/status")
    public String updateStatus(
            @PathVariable Long deliveryId,
            @RequestParam("status") DeliveryStatus status,
            RedirectAttributes redirectAttributes) {
        Delivery updated = deliveryService.updateManualStatus(deliveryId, status);
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Delivery " + updated.getOrder().getReceiptNumber() + " updated to " + status.getLabel() + ".");
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/{deliveryId}/simulate-webhook")
    public String simulateWebhookStep(@PathVariable Long deliveryId, RedirectAttributes redirectAttributes) {
        try {
            Delivery updated = deliveryService.simulateNextWebhook(deliveryId);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Simulated courier webhook → " + updated.getStatus().getLabel()
                            + (updated.hasRiderAssigned() ? " (" + updated.getRiderName() + ")" : "") + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }
}
