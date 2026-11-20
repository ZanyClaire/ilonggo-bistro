package com.ilonggobistro.admin;

import com.ilonggobistro.menu.MenuItem;
import com.ilonggobistro.menu.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuService menuService;

    @Test
    void unauthenticatedRequestsToAdminRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanAccessAllStaffPagesAndToggleMenuAvailabilityViaHtmx() throws Exception {
        for (String route : new String[]{
                "/admin", "/admin/pos", "/admin/kitchen", "/admin/deliveries", "/admin/sales", "/admin/categories",
                "/admin/events", "/admin/menu", "/admin/reservations", "/admin/gallery",
                "/admin/inquiries", "/admin/staff", "/admin/account", "/admin/settings"}) {
            mockMvc.perform(get(route))
                    .andExpect(status().isOk());
        }

        MenuItem firstItem = menuService.listAllItems().getFirst();
        boolean wasAvailable = firstItem.isAvailable();

        mockMvc.perform(post("/admin/menu/{id}/toggle-availability", firstItem.getId())
                        .with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(wasAvailable ? "Sold Out" : "Available")));

        // Toggle back so test data remains clean
        mockMvc.perform(post("/admin/menu/{id}/toggle-availability", firstItem.getId())
                        .with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk());
    }
}
