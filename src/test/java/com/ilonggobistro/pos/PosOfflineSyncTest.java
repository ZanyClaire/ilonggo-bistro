package com.ilonggobistro.pos;

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
class PosOfflineSyncTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuService menuService;

    @Autowired
    private PosOrderRepository orderRepository;

    @Test
    void pwaManifestAndServiceWorkerAreAccessible() throws Exception {
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ilonggo Bistro POS")));

        mockMvc.perform(get("/sw-pos.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ilonggo-bistro-pos-v1")));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void cashierCanFetchCatalogSyncOfflineBatchIdempotentlyAndSearchOfflineReceipt() throws Exception {
        mockMvc.perform(get("/admin/pos/catalog.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store.bistroName").value("Ilonggo Bistro"))
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/admin/pos/csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        MenuItem item = menuService.listAllItems().getFirst();
        String clientUuid = "uuid-offline-test-1001";
        String offlineRef = "OR-OFF-2026-TST01";

        String batchJson = """
                {
                  "orders": [
                    {
                      "clientOrderUuid": "%s",
                      "offlineReceiptNumber": "%s",
                      "createdAtIso": "2026-11-20T02:15:00Z",
                      "customerName": "Mang Tomas Offline",
                      "customerPhone": "0917 222 3344",
                      "orderType": "DINE_IN",
                      "tableNumber": "Table 4",
                      "paymentMethod": "CASH",
                      "discountType": "SENIOR_PWD",
                      "customDiscountPhp": 0,
                      "amountTenderedPhp": 500.00,
                      "notes": "Served during Wi-Fi outage",
                      "items": [
                        {
                          "menuItemId": %d,
                          "itemName": "%s",
                          "unitPricePhp": %s,
                          "quantity": 2
                        }
                      ]
                    }
                  ]
                }
                """.formatted(clientUuid, offlineRef, item.getId(), item.getName(), item.getPricePhp());

        // 1. First sync succeeds and creates server order
        MvcResult firstSync = mockMvc.perform(post("/admin/pos/sync")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(1))
                .andExpect(jsonPath("$.alreadySyncedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.results[0].status").value("SYNCED"))
                .andReturn();

        String serverReceipt = firstSync.getResponse().getContentAsString()
                .replaceAll(".*\"serverReceiptNumber\":\"([^\"]+)\".*", "$1");
        assertThat(serverReceipt).startsWith("OR-");

        PosOrder persisted = orderRepository.findWithItemsByClientOrderUuid(clientUuid).orElseThrow();
        assertThat(persisted.isSyncedFromOffline()).isTrue();
        assertThat(persisted.getOfflineReceiptNumber()).isEqualTo(offlineRef);

        // 2. Retrying exact same UUID is 100% idempotent (zero duplicate order)
        mockMvc.perform(post("/admin/pos/sync")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(0))
                .andExpect(jsonPath("$.alreadySyncedCount").value(1))
                .andExpect(jsonPath("$.results[0].status").value("ALREADY_SYNCED"))
                .andExpect(jsonPath("$.results[0].serverReceiptNumber").value(serverReceipt));

        // 3. Offline receipt number is searchable in Sales History & visible on Receipt page
        mockMvc.perform(get("/admin/sales").param("q", offlineRef))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mang Tomas Offline")))
                .andExpect(content().string(containsString("Offline Synced")))
                .andExpect(content().string(containsString(offlineRef)));

        mockMvc.perform(get("/admin/pos/receipt/{ref}", serverReceipt))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(offlineRef)));
    }

    @Test
    @WithMockUser(username = "kitchen", roles = {"KITCHEN_STAFF"})
    void kitchenStaffCannotSyncOfflinePosOrders() throws Exception {
        mockMvc.perform(post("/admin/pos/sync")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orders\":[]}"))
                .andExpect(status().isForbidden());
    }
}
