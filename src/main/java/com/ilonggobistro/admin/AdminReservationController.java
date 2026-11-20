package com.ilonggobistro.admin;

import com.ilonggobistro.reservation.Reservation;
import com.ilonggobistro.reservation.ReservationService;
import com.ilonggobistro.reservation.ReservationStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public String listReservations(
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String statusFilter,
            Model model) {
        model.addAttribute("reservations", reservationService.listAll(statusFilter));
        model.addAttribute("activeStatus", statusFilter.toUpperCase());
        model.addAttribute("statuses", ReservationStatus.values());
        return "admin/reservations";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam("status") ReservationStatus status,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        Reservation updated = reservationService.updateStatus(id, status);
        if ("true".equals(hxRequest)) {
            model.addAttribute("res", updated);
            model.addAttribute("statuses", ReservationStatus.values());
            return "admin/fragments/reservation-row :: row";
        }
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Reservation " + updated.getReferenceCode() + " updated to " + status.getLabel() + ".");
        return "redirect:/admin/reservations";
    }
}
