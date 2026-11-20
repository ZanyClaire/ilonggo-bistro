package com.ilonggobistro.shared;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessHoursServiceTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private StoreSettingsService stubSettings() {
        StoreSettingsService settings = mock(StoreSettingsService.class);
        when(settings.getProfile()).thenReturn(new StoreProfile(
                "Ilonggo Bistro", "Tagline", "Burgos-Osmeña St", "VPW2+QF", "https://maps.google.com",
                LocalTime.of(7, 0), LocalTime.of(21, 0),
                "+63 917 000 0000", "hello@ilonggobistro.ph", "https://fb.com", "https://m.me",
                "", 20, 40
        ));
        return settings;
    }

    @Test
    void reportsOpenDuringDaytimeInManila() {
        // 12:30 PM Asia/Manila == 04:30 UTC
        Clock fixed = Clock.fixed(Instant.parse("2026-11-19T04:30:00Z"), MANILA);
        BusinessHoursService service = new BusinessHoursService(fixed, stubSettings());

        BusinessHoursService.BusinessStatus status = service.getCurrentStatus();
        assertThat(status.open()).isTrue();
        assertThat(status.tone()).isEqualTo("open");
        assertThat(status.headline()).contains("Open now").contains("9 PM");
    }

    @Test
    void reportsClosingSoonWithin45MinutesOfClosing() {
        // 8:30 PM Asia/Manila == 12:30 UTC
        Clock fixed = Clock.fixed(Instant.parse("2026-11-19T12:30:00Z"), MANILA);
        BusinessHoursService service = new BusinessHoursService(fixed, stubSettings());

        BusinessHoursService.BusinessStatus status = service.getCurrentStatus();
        assertThat(status.open()).isTrue();
        assertThat(status.tone()).isEqualTo("closing-soon");
        assertThat(status.headline()).contains("Closes soon");
    }

    @Test
    void reportsClosedAfter9Pm() {
        // 9:15 PM Asia/Manila == 13:15 UTC
        Clock fixed = Clock.fixed(Instant.parse("2026-11-19T13:15:00Z"), MANILA);
        BusinessHoursService service = new BusinessHoursService(fixed, stubSettings());

        BusinessHoursService.BusinessStatus status = service.getCurrentStatus();
        assertThat(status.open()).isFalse();
        assertThat(status.tone()).isEqualTo("closed");
        assertThat(status.headline()).contains("Opens 7 AM");
    }

    @Test
    void generatesHalfHourlySlotsBetween7AmAnd830PmForFutureDates() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-19T04:30:00Z"), MANILA);
        BusinessHoursService service = new BusinessHoursService(fixed, stubSettings());

        List<LocalTime> slots = service.getValidReservationSlots(LocalDate.of(2026, 11, 20));
        assertThat(slots).isNotEmpty();
        assertThat(slots.getFirst()).isEqualTo(LocalTime.of(7, 0));
        assertThat(slots.getLast()).isEqualTo(LocalTime.of(20, 30));
    }
}
