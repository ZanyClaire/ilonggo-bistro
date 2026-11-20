package com.ilonggobistro.menu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersFullMenuPageWithSeededIlonggoDishes() throws Exception {
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Signature La Paz Batchoy")))
                .andExpect(content().string(containsString("Ilonggo Chicken Inasal Pecho")));
    }

    @Test
    void filtersMenuFragmentByCategoryAndQuery() throws Exception {
        mockMvc.perform(get("/menu/fragment")
                        .param("category", "drinks-coffee")
                        .param("q", "Tablea"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iced Iloilo Tablea Mocha")))
                .andExpect(content().string(not(containsString("Signature La Paz Batchoy"))));
    }
}
