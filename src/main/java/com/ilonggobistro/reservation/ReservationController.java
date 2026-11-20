package com.ilonggobistro.reservation;

import com.ilonggobistro.shared.BusinessHoursService;
import com.ilonggobistro.shared.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private static final List<String> SEATING_OPTIONS = List.of(
            "Main Dining",
            "Window Table",
            "Family / Barkada Table",
            "Quiet Coffee Corner"
    );

    private final ReservationService reservationService;
    private final BusinessHoursService businessHoursService;

    public ReservationController(ReservationService reservationService,
                                 BusinessHoursService businessHoursService) {
        this.reservationService = reservationService;
        this.businessHoursService = businessHoursService;
    }

    @GetMapping
    public String reservationForm(
            @RequestParam(name = "ref", required = false) String lookupRef,
            Model model) {
        LocalDate today = businessHoursService.today();
        List<LocalTime> todaySlots = businessHoursService.getValidReservationSlots(today);
        LocalDate defaultDate = todaySlots.isEmpty() ? today.plusDays(1) : today;
        List<LocalTime> slots = todaySlots.isEmpty()
                ? businessHoursService.getValidReservationSlots(defaultDate)
                : todaySlots;

        if (!model.containsAttribute("reservationRequest")) {
            ReservationRequest req = new ReservationRequest();
            req.setReservationDate(defaultDate);
            if (!slots.isEmpty()) {
                req.setReservationTime(slots.getFirst());
            }
            model.addAttribute("reservationRequest", req);
        }

        populateFormHelpers(defaultDate, slots, model);

        if (lookupRef != null && !lookupRef.isBlank()) {
            model.addAttribute("lookupRef", lookupRef.trim());
            reservationService.findByReferenceCode(lookupRef)
                    .ifPresentOrElse(
                            found -> model.addAttribute("foundReservation", found),
                            () -> model.addAttribute("lookupNotFound", true)
                    );
        }
        return "pages/reservations";
    }

    @GetMapping("/slots")
    public String timeSlotsFragment(
            @RequestParam(name = "reservationDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate targetDate = date != null ? date : businessHoursService.today();
        List<LocalTime> slots = businessHoursService.getValidReservationSlots(targetDate);
        model.addAttribute("timeSlots", slots);
        model.addAttribute("selectedTime", slots.isEmpty() ? null : slots.getFirst());
        return "fragments/time-slots :: options";
    }

    @PostMapping
    public String submitReservation(
            @Valid @ModelAttribute("reservationRequest") ReservationRequest request,
            BindingResult bindingResult,
            Model model) {
        LocalDate targetDate = request.getReservationDate() != null
                ? request.getReservationDate()
                : businessHoursService.today();
        List<LocalTime> slots = businessHoursService.getValidReservationSlots(targetDate);

        if (bindingResult.hasErrors()) {
            populateFormHelpers(targetDate, slots, model);
            return "pages/reservations";
        }

        try {
            Reservation saved = reservationService.createReservation(request);
            return "redirect:/reservations/confirmed/" + saved.getReferenceCode();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("bookingError", ex.getMessage());
            populateFormHelpers(targetDate, slots, model);
            return "pages/reservations";
        }
    }

    @GetMapping("/confirmed/{referenceCode}")
    public String confirmed(@PathVariable String referenceCode, Model model) {
        Reservation reservation = reservationService.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation reference " + referenceCode + " was not found."));
        model.addAttribute("reservation", reservation);
        return "pages/reservation-confirmed";
    }

    @PostMapping("/{referenceCode}/cancel")
    public String cancelReservation(
            @PathVariable String referenceCode,
            @RequestParam("phoneLastDigits") String phoneLastDigits,
            RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancelByGuest(referenceCode, phoneLastDigits);
            redirectAttributes.addFlashAttribute("lookupMessage",
                    "Reservation " + referenceCode + " has been cancelled.");
        } catch (IllegalArgumentException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("lookupError", ex.getMessage());
        }
        return "redirect:/reservations?ref=" + referenceCode + "#lookup";
    }

    private void populateFormHelpers(LocalDate date, List<LocalTime> slots, Model model) {
        LocalDate today = businessHoursService.today();
        model.addAttribute("minDate", today);
        model.addAttribute("maxDate", today.plusDays(60));
        model.addAttribute("selectedDate", date);
        model.addAttribute("timeSlots", slots);
        model.addAttribute("seatingOptions", SEATING_OPTIONS);
    }
}
