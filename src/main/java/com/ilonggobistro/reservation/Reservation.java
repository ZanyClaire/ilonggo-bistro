package com.ilonggobistro.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "reservations")
public class Reservation {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_code", nullable = false, unique = true, length = 24)
    private String referenceCode;

    @Column(name = "guest_name", nullable = false, length = 120)
    private String guestName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    @Column(name = "seating_preference", nullable = false, length = 60)
    private String seatingPreference = "Main Dining";

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Reservation() {
    }

    public Reservation(String referenceCode, String guestName, String phone, String email,
                       int partySize, LocalDate reservationDate, LocalTime reservationTime,
                       String seatingPreference, String specialRequests) {
        this.referenceCode = referenceCode;
        this.guestName = guestName;
        this.phone = phone;
        this.email = email;
        this.partySize = partySize;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
        this.seatingPreference = seatingPreference;
        this.specialRequests = specialRequests;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReservationStatus.PENDING;
        }
    }

    public String formattedDate() {
        return reservationDate != null ? reservationDate.format(DATE_FMT) : "";
    }

    public String formattedTime() {
        return reservationTime != null ? reservationTime.format(TIME_FMT) : "";
    }

    public boolean isCancellableByGuest() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public Long getId() {
        return id;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public int getPartySize() {
        return partySize;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public LocalTime getReservationTime() {
        return reservationTime;
    }

    public String getSeatingPreference() {
        return seatingPreference;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
