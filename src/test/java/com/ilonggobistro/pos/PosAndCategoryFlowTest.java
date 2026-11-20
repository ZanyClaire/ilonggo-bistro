package com.ilonggobistro.pos;

import com.ilonggobistro.menu.MenuCategory;
import com.ilonggobistro.menu.MenuCategoryRepository;
import com.ilonggobistro.menu.MenuItem;
import com.ilonggobistro.menu.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PosAndCategoryFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Test
    @WithMockUser(username = "cashier1", roles = {"ADMIN"})
    void adminCanCheckoutPosOrderViewReceiptSearchBuyerHistoryAndVoid() throws Exception {
        MenuItem item = menuService.listAllItems().getFirst(); // Signature La Paz Batchoy (₱195.00)

        String jsonPayload = """
                {
                  "customerName": "Lola Remedios",
                  "customerPhone": "0917 889 1020",
                  "orderType": "DINE_IN",
                  "tableNumber": "Table 5",
                  "paymentMethod": "CASH",
                  "discountType": "SENIOR_PWD",
                  "amountTenderedPhp": 500.00,
                  "notes": "Hot broth please",
                  "items": [
                    { "menuItemId": %d, "quantity": 2 }
                  ]
                }
                """.formatted(item.getId());

        MvcResult result = mockMvc.perform(post("/admin/pos/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String receiptNumber = body.replaceAll(".*\"receiptNumber\":\"([^\"]+)\".*", "$1");
        assertThat(receiptNumber).startsWith("OR-");

        // Verify receipt page renders buyer name, Senior/PWD discount, and change (390 - 78 = 312 -> 500 - 312 = 188)
        mockMvc.perform(get("/admin/pos/receipt/{ref}", receiptNumber))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lola Remedios")))
                .andExpect(content().string(containsString("Senior / PWD (20%)")))
                .andExpect(content().string(containsString("₱312.00")))
                .andExpect(content().string(containsString("₱188.00")));

        // Verify Sales & Buyer History search finds the buyer
        mockMvc.perform(get("/admin/sales").param("q", "Lola Remedios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(receiptNumber)))
                .andExpect(content().string(containsString("Lola Remedios")))
                .andExpect(content().string(containsString("Table 5")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanCreateAndDeleteCustomCategory() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", "Iloilo Seafood Specials")
                        .param("daypart", "Lunch & Dinner")
                        .param("description", "Fresh catch grilled and simmered.")
                        .param("displayOrder", "9"))
                .andExpect(status().is3xxRedirection());

        MenuCategory created = categoryRepository.findBySlug("iloilo-seafood-specials").orElseThrow();
        assertThat(created.getName()).isEqualTo("Iloilo Seafood Specials");

        // Verify new category appears on public Menu and POS
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iloilo Seafood Specials")));

        mockMvc.perform(get("/admin/pos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iloilo Seafood Specials")));

        // Delete the empty custom category cleanly
        mockMvc.perform(post("/admin/categories/{id}/delete", created.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(categoryRepository.findBySlug("iloilo-seafood-specials")).isEmpty();
    }
}
