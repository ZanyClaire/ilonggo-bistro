package com.ilonggobistro.reservation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationRequest {

    @NotBlank(message = "Please enter your full name")
    @Size(max = 120, message = "Name must be 120 characters or fewer")
    private String guestName;

    @NotBlank(message = "Mobile or phone number is required")
    @Pattern(regexp = "^[+0-9()\\s-]{7,25}$", message = "Enter a valid phone number (e.g., 0917 123 4567)")
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 160)
    private String email;

    @NotNull(message = "Please choose party size")
    @Min(value = 1, message = "Minimum party size is 1 guest")
    @Max(value = 20, message = "For groups over 20 guests, please contact us directly for private dining")
    private Integer partySize = 2;

    @NotNull(message = "Please pick a date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reservationDate;

    @NotNull(message = "Please pick a time slot")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime reservationTime;

    @NotBlank(message = "Please select a seating area")
    private String seatingPreference = "Main Dining";

    @Size(max = 600, message = "Special requests must be 600 characters or fewer")
    private String specialRequests;

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public LocalTime getReservationTime() {
        return reservationTime;
    }

    public void setReservationTime(LocalTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    public String getSeatingPreference() {
        return seatingPreference;
    }

    public void setSeatingPreference(String seatingPreference) {
        this.seatingPreference = seatingPreference;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }
}
