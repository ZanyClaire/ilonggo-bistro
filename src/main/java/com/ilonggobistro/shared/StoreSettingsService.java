package com.ilonggobistro.shared;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StoreSettingsService {

    private final StoreSettingRepository repository;

    public StoreSettingsService(StoreSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public StoreProfile getProfile() {
        Map<String, String> map = repository.findAll().stream()
                .collect(Collectors.toMap(StoreSetting::getKey, StoreSetting::getValue, (a, b) -> b));

        return new StoreProfile(
                map.getOrDefault("bistro_name", "Ilonggo Bistro"),
                map.getOrDefault("tagline", "Warm Ilonggo Comfort, From First Coffee to Last Call"),
                map.getOrDefault("address_line", "Burgos-Osmeña St, Barotac Nuevo, 5007 Iloilo"),
                map.getOrDefault("plus_code", "VPW2+QF Barotac Nuevo, Iloilo"),
                map.getOrDefault("google_maps_url", "https://www.google.com/maps/search/?api=1&query=VPW2%2BQF+Barotac+Nuevo%2C+Iloilo"),
                parseTime(map.get("open_time"), LocalTime.of(7, 0)),
                parseTime(map.get("close_time"), LocalTime.of(21, 0)),
                map.getOrDefault("phone_number", "+63 917 000 0000"),
                map.getOrDefault("email", "hello@ilonggobistro.ph"),
                map.getOrDefault("facebook_url", "https://facebook.com"),
                map.getOrDefault("messenger_url", "https://m.me"),
                map.getOrDefault("announcement_banner", ""),
                parseInt(map.get("max_party_size"), 20),
                parseInt(map.get("hourly_seat_capacity"), 40)
        );
    }

    @Transactional
    public void updateSettings(Map<String, String> updates) {
        updates.forEach((key, value) -> {
            if (key != null && value != null) {
                StoreSetting setting = repository.findById(key)
                        .orElseGet(() -> new StoreSetting(key, value.trim()));
                setting.setValue(value.trim());
                repository.save(setting);
            }
        });
    }

    private LocalTime parseTime(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
