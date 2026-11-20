package com.ilonggobistro.shared;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BusinessHoursService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_HOUR = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_SHORT = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private final Clock clock;
    private final StoreSettingsService settingsService;

    public BusinessHoursService(Clock clock, StoreSettingsService settingsService) {
        this.clock = clock;
        this.settingsService = settingsService;
    }

    public BusinessStatus getCurrentStatus() {
        StoreProfile profile = settingsService.getProfile();
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalTime currentTime = now.toLocalTime();
        LocalTime openTime = profile.openTime();
        LocalTime closeTime = profile.closeTime();

        String hoursLabel = formatFriendlyTime(openTime) + " – " + formatFriendlyTime(closeTime);
        String daypart = resolveDaypart(currentTime);

        boolean isOpen = !currentTime.isBefore(openTime) && currentTime.isBefore(closeTime);
        if (isOpen) {
            long minutesUntilClose = Duration.between(currentTime, closeTime).toMinutes();
            if (minutesUntilClose <= 45) {
                return new BusinessStatus(
                        true,
                        "closing-soon",
                        "Closes soon · " + formatFriendlyTime(closeTime),
                        "Last orders accepted 30 minutes before closing",
                        hoursLabel,
                        daypart
                );
            }
            return new BusinessStatus(
                    true,
                    "open",
                    "Open now · Until " + formatFriendlyTime(closeTime),
                    "Walk-ins & dine-in reservations welcome",
                    hoursLabel,
                    daypart
            );
        }

        String nextOpenDay = currentTime.isBefore(openTime)
                ? "today"
                : now.plusDays(1).format(DAY_SHORT);

        return new BusinessStatus(
                false,
                "closed",
                "Closed · Opens " + formatFriendlyTime(openTime) + " " + nextOpenDay,
                "Reserve your table ahead for breakfast, lunch, or dinner",
                hoursLabel,
                daypart
        );
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public List<LocalTime> getValidReservationSlots(LocalDate date) {
        StoreProfile profile = settingsService.getProfile();
        LocalDate today = LocalDate.now(clock);
        if (date == null || date.isBefore(today)) {
            return List.of();
        }

        LocalTime openTime = profile.openTime();
        LocalTime lastSeating = profile.closeTime().minusMinutes(30);
        LocalTime earliestAllowed = openTime;

        if (date.equals(today)) {
            LocalTime cutoffToday = ZonedDateTime.now(clock).toLocalTime().plusMinutes(30);
            if (cutoffToday.isAfter(earliestAllowed)) {
                earliestAllowed = cutoffToday;
            }
        }

        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = openTime;
        while (!cursor.isAfter(lastSeating)) {
            if (!cursor.isBefore(earliestAllowed)) {
                slots.add(cursor);
            }
            cursor = cursor.plusMinutes(30);
        }
        return slots;
    }

    public boolean isValidSlot(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return false;
        }
        return getValidReservationSlots(date).contains(time);
    }

    public String formatFriendlyTime(LocalTime time) {
        return time.getMinute() == 0 ? time.format(SHORT_HOUR) : time.format(DISPLAY_TIME);
    }

    private String resolveDaypart(LocalTime time) {
        if (time.isBefore(LocalTime.of(11, 0))) {
            return "Morning Breakfast & Kapehan";
        }
        if (time.isBefore(LocalTime.of(14, 0))) {
            return "Ilonggo Lunch Classics";
        }
        if (time.isBefore(LocalTime.of(17, 30))) {
            return "Afternoon Merienda & Coffee";
        }
        return "Evening Bistro Dinner";
    }

    public record BusinessStatus(
            boolean open,
            String tone,
            String headline,
            String subline,
            String hoursRange,
            String currentDaypart
    ) {
    }
}
