package com.ilonggobistro.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessEventPhotoFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessEventRepository eventRepository;

    @Test
    void publicEventsHubAndDetailPagesRenderCleanly() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Barotac Nuevo Hospitality Awards Night")));

        mockMvc.perform(get("/events/fragment").param("type", "Catering"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Burgos-Osmeña Fiesta Private Catering")))
                .andExpect(content().string(not(containsString("Hospitality Awards Night"))));

        mockMvc.perform(get("/events/barotac-nuevo-hospitality-awards-night"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Share your photos from this event")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanCreateEventWithBatchPhotosAndModerateGuestBatchUploads() throws Exception {
        MockMultipartFile adminImg1 = new MockMultipartFile("files", "stage.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        MockMultipartFile adminImg2 = new MockMultipartFile("files", "dinner.png", "image/png", new byte[]{5, 6, 7, 8});

        // 1. Admin creates a new Business Event with 2 initial photos in one batch
        mockMvc.perform(multipart("/admin/events")
                        .file(adminImg1)
                        .file(adminImg2)
                        .with(csrf())
                        .param("title", "Iloilo Food Festival Gala")
                        .param("eventType", "Awards Night")
                        .param("eventDate", "2026-12-05")
                        .param("location", "Ilonggo Bistro Main Hall")
                        .param("description", "Annual gala dinner.")
                        .param("allowGuestUploads", "true")
                        .param("published", "true"))
                .andExpect(status().is3xxRedirection());

        BusinessEvent created = eventRepository.findWithPhotosBySlugIgnoreCaseAndPublishedTrue("iloilo-food-festival-gala")
                .orElseThrow();
        assertThat(created.approvedPhotoCount()).isEqualTo(2);
        assertThat(created.getCoverImageUrl()).startsWith("/uploads/");

        // 2. Guest uploads 2 photos at once to that specific event
        MockMultipartFile guestImg1 = new MockMultipartFile("files", "table1.jpg", "image/jpeg", new byte[]{9, 10, 11});
        MockMultipartFile guestImg2 = new MockMultipartFile("files", "table2.webp", "image/webp", new byte[]{12, 13, 14});

        mockMvc.perform(multipart("/events/{slug}/upload", created.getSlug())
                        .file(guestImg1)
                        .file(guestImg2)
                        .with(csrf())
                        .param("guestName", "Katrina Soriano")
                        .param("caption", "Best gala dessert table!"))
                .andExpect(status().is3xxRedirection());

        BusinessEvent afterGuest = eventRepository.findWithPhotosById(created.getId()).orElseThrow();
        assertThat(afterGuest.pendingPhotoCount()).isEqualTo(2);
        assertThat(afterGuest.approvedPhotoCount()).isEqualTo(2);

        // Public page does NOT display unapproved guest caption yet
        mockMvc.perform(get("/events/{slug}", created.getSlug()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Best gala dessert table!"))));

        // 3. Admin views event manager (sees pending guest photos) and approves one
        mockMvc.perform(get("/admin/events/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pending Guest Uploads")))
                .andExpect(content().string(containsString("Katrina Soriano")));

        Long pendingPhotoId = afterGuest.pendingPhotos().getFirst().getId();
        mockMvc.perform(post("/admin/events/{eid}/photos/{pid}/approve", created.getId(), pendingPhotoId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Now approved guest photo & caption appear publicly!
        mockMvc.perform(get("/events/{slug}", created.getSlug()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Best gala dessert table!")))
                .andExpect(content().string(containsString("Katrina Soriano")));
    }
}
