package com.ilonggobistro.home;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicPagesSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allPublicPagesAndSitemapRenderCleanly() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Burgos-Osmeña St")))
                .andExpect(content().string(containsString("VPW2+QF")));

        mockMvc.perform(get("/gallery"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Food, drink")));

        mockMvc.perform(get("/gallery/fragment").param("category", "Vibe"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Morning Sunlight")));

        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Weekly Opening Hours")));

        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Contact")));

        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign In to Portal")));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<urlset")));
    }
}
