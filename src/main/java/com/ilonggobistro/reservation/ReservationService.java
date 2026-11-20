package com.ilonggobistro.reservation;

import com.ilonggobistro.shared.BusinessHoursService;
import com.ilonggobistro.shared.ResourceNotFoundException;
import com.ilonggobistro.shared.StoreProfile;
import com.ilonggobistro.shared.StoreSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReservationService {

    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<ReservationStatus> ACTIVE_STATUSES = Set.of(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED,
            ReservationStatus.SEATED
    );

    private final ReservationRepository repository;
    private final BusinessHoursService businessHoursService;
    private final StoreSettingsService storeSettingsService;

    public ReservationService(ReservationRepository repository,
                              BusinessHoursService businessHoursService,
                              StoreSettingsService storeSettingsService) {
        this.repository = repository;
        this.businessHoursService = businessHoursService;
        this.storeSettingsService = storeSettingsService;
    }

    @Transactional
    public Reservation createReservation(ReservationRequest request) {
        StoreProfile profile = storeSettingsService.getProfile();
        LocalDate today = businessHoursService.today();

        if (request.getReservationDate() == null || request.getReservationDate().isBefore(today)) {
            throw new IllegalArgumentException("Please pick today or a future date.");
        }
        if (request.getReservationDate().isAfter(today.plusDays(60))) {
            throw new IllegalArgumentException("Reservations can be booked up to 60 days ahead.");
        }
        if (!businessHoursService.isValidSlot(request.getReservationDate(), request.getReservationTime())) {
            throw new IllegalArgumentException("Selected time slot is outside our reservation hours ("
                    + businessHoursService.getCurrentStatus().hoursRange() + ", last seating 30 mins before close).");
        }
        if (request.getPartySize() > profile.maxPartySize()) {
            throw new IllegalArgumentException("For parties larger than " + profile.maxPartySize()
                    + " guests, please reach out through our Contact page for private dining.");
        }

        LocalTime windowStart = request.getReservationTime().minusMinutes(30);
        LocalTime windowEnd = request.getReservationTime().plusMinutes(30);
        int reservedSeats = repository.sumSeatsInWindow(
                request.getReservationDate(), windowStart, windowEnd, ACTIVE_STATUSES
        );
        if (reservedSeats + request.getPartySize() > profile.hourlySeatCapacity()) {
            throw new IllegalArgumentException("That time slot is nearly full. Please try an adjacent 30-minute slot.");
        }

        String refCode = generateUniqueReferenceCode(request.getReservationDate().getYear());
        Reservation reservation = new Reservation(
                refCode,
                request.getGuestName().trim(),
                request.getPhone().trim(),
                request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null,
                request.getPartySize(),
                request.getReservationDate(),
                request.getReservationTime(),
                request.getSeatingPreference(),
                request.getSpecialRequests() != null && !request.getSpecialRequests().isBlank()
                        ? request.getSpecialRequests().trim() : null
        );
        return repository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Optional<Reservation> findByReferenceCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return repository.findByReferenceCodeIgnoreCase(code.trim());
    }

    @Transactional
    public Reservation cancelByGuest(String referenceCode, String phoneLastDigits) {
        Reservation reservation = repository.findByReferenceCodeIgnoreCase(referenceCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation reference not found."));
        String storedDigits = reservation.getPhone().replaceAll("\\D", "");
        String providedDigits = phoneLastDigits == null ? "" : phoneLastDigits.replaceAll("\\D", "");
        if (providedDigits.length() < 4 || !storedDigits.endsWith(providedDigits)) {
            throw new IllegalArgumentException("Phone number digits did not match this reservation.");
        }
        if (!reservation.isCancellableByGuest()) {
            throw new IllegalArgumentException("This reservation can no longer be cancelled online.");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return repository.save(reservation);
    }

    @Transactional
    public Reservation updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found."));
        reservation.setStatus(status);
        return repository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> listForDate(LocalDate date) {
        return repository.findByReservationDateOrderByReservationTimeAscCreatedAtAsc(date);
    }

    @Transactional(readOnly = true)
    public List<Reservation> listUpcoming() {
        return repository.findByReservationDateGreaterThanEqualOrderByReservationDateAscReservationTimeAsc(
                businessHoursService.today());
    }

    @Transactional(readOnly = true)
    public List<Reservation> listAll(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                ReservationStatus status = ReservationStatus.valueOf(statusFilter.trim().toUpperCase());
                return repository.findByStatusOrderByReservationDateAscReservationTimeAsc(status);
            } catch (IllegalArgumentException ignored) {
                // fall through to all
            }
        }
        return repository.findAllByOrderByReservationDateDescReservationTimeDesc();
    }

    @Transactional(readOnly = true)
    public long countTodayActive() {
        return repository.countByReservationDateAndStatusNot(
                businessHoursService.today(), ReservationStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByStatus(ReservationStatus.PENDING);
    }

    private String generateUniqueReferenceCode(int year) {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder suffix = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                suffix.append(REF_CHARS.charAt(RANDOM.nextInt(REF_CHARS.length())));
            }
            String candidate = "IB-" + year + "-" + suffix;
            if (!repository.existsByReferenceCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique reservation reference code.");
    }
}
