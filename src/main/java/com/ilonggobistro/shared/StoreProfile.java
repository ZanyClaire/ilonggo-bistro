package com.ilonggobistro.shared;

import java.time.LocalTime;

public record StoreProfile(
        String bistroName,
        String tagline,
        String addressLine,
        String plusCode,
        String googleMapsUrl,
        LocalTime openTime,
        LocalTime closeTime,
        String phoneNumber,
        String email,
        String facebookUrl,
        String messengerUrl,
        String announcementBanner,
        int maxPartySize,
        int hourlySeatCapacity
) {
    public String formattedTelHref() {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "";
        }
        return "tel:" + phoneNumber.replaceAll("[^+0-9]", "");
    }
}
