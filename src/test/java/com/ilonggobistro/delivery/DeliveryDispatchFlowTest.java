package com.ilonggobistro.delivery;

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
class DeliveryDispatchFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuService menuService;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Test
    void orderCheckoutPageRendersWithMapAndMenu() throws Exception {
        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Drop-off Location Pin")))
                .andExpect(content().string(containsString("/api/delivery/quote")));
    }

    @Test
    void deliveryQuoteCalculatesLocalFeeAndDetectsInHouseFallback() throws Exception {
        // Near Barotac Nuevo (~1.2 km from 10.8969, 122.7022)
        mockMvc.perform(post("/api/delivery/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customerLat": 10.8920, "customerLng": 122.7110 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliverable").value(true))
                .andExpect(jsonPath("$.inHouseFallback").value(false))
                .andExpect(jsonPath("$.deliveryFeePhp").isNumber());

        // ~10.5 km away (outside 8 km courier zone, inside 15 km in-house zone)
        mockMvc.perform(post("/api/delivery/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customerLat": 10.9850, "customerLng": 122.7350 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliverable").value(true))
                .andExpect(jsonPath("$.inHouseFallback").value(true))
                .andExpect(jsonPath("$.provider").value("IN_HOUSE"));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"ADMIN"})
    void endToEndCheckoutDispatchWebhookAndCustomerLiveTracking() throws Exception {
        MenuItem item = menuService.listAllItems().getFirst();

        // 1. Customer submits online delivery order
        String orderJson = """
                {
                  "customerName": "Paolo Gamboa",
                  "customerPhone": "0917 302 8891",
                  "fulfillmentType": "DELIVERY",
                  "paymentMode": "CASH_ON_DELIVERY",
                  "customerLat": 10.8935,
                  "customerLng": 122.7095,
                  "dropoffAddress": "Brgy. Ilaud Poblacion, Barotac Nuevo, Iloilo",
                  "dropoffLandmark": "White fence across basketball court",
                  "notes": "Extra calamansi please",
                  "items": [
                    { "menuItemId": %d, "quantity": 2 }
                  ]
                }
                """.formatted(item.getId());

        MvcResult checkoutResult = mockMvc.perform(post("/order/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").exists())
                .andReturn();

        String receipt = checkoutResult.getResponse().getContentAsString()
                .replaceAll(".*\"receiptNumber\":\"([^\"]+)\".*", "$1");

        Delivery created = deliveryRepository.findWithOrderByOrderReceiptNumberIgnoreCase(receipt).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(DeliveryStatus.PENDING_DISPATCH);

        // 2. Cashier clicks "Dispatch Rider"
        mockMvc.perform(post("/admin/deliveries/{orderId}/dispatch", created.getOrder().getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Delivery dispatched = deliveryRepository.findWithOrderByOrderReceiptNumberIgnoreCase(receipt).orElseThrow();
        assertThat(dispatched.getStatus()).isEqualTo(DeliveryStatus.ALLOCATING);
        assertThat(dispatched.getExternalDeliveryId()).isNotBlank();

        // 3. Courier webhook posts RIDER_ASSIGNED without CSRF cookie
        String webhookJson = """
                {
                  "externalDeliveryId": "%s",
                  "orderReference": "%s",
                  "status": "RIDER_ASSIGNED",
                  "riderName": "Kuya Marco Espinosa",
                  "riderPhone": "0998 551 7721",
                  "plateNumber": "ILO-8821",
                  "trackingUrl": "https://track.example.com/%s"
                }
                """.formatted(dispatched.getExternalDeliveryId(), receipt, dispatched.getExternalDeliveryId());

        mockMvc.perform(post("/api/webhooks/delivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RIDER_ASSIGNED"));

        // 4. Customer tracking page & HTMX polling fragment display assigned rider & Track Rider button
        mockMvc.perform(get("/orders/{ref}", receipt))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kuya Marco Espinosa")))
                .andExpect(content().string(containsString("ILO-8821")))
                .andExpect(content().string(containsString("Track Rider")));

        mockMvc.perform(get("/orders/{ref}/status-fragment", receipt))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rider Assigned")));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"ADMIN"})
    void adminCanAssignInHouseLocalRiderAndAdvanceManualStatus() throws Exception {
        MenuItem item = menuService.listAllItems().getFirst();

        String farOrderJson = """
                {
                  "customerName": "Tita Lorna",
                  "customerPhone": "0928 112 9044",
                  "fulfillmentType": "DELIVERY",
                  "paymentMode": "PAID",
                  "customerLat": 10.9750,
                  "customerLng": 122.7250,
                  "dropoffAddress": "Brgy. Tinorian, Barotac Nuevo, Iloilo",
                  "dropoffLandmark": "Near barangay hall",
                  "items": [
                    { "menuItemId": %d, "quantity": 1 }
                  ]
                }
                """.formatted(item.getId());

        MvcResult res = mockMvc.perform(post("/order/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(farOrderJson))
                .andExpect(status().isOk())
                .andReturn();

        String receipt = res.getResponse().getContentAsString()
                .replaceAll(".*\"receiptNumber\":\"([^\"]+)\".*", "$1");
        Delivery del = deliveryRepository.findWithOrderByOrderReceiptNumberIgnoreCase(receipt).orElseThrow();

        // Assign In-House Local Rider
        mockMvc.perform(post("/admin/deliveries/{oid}/assign-inhouse", del.getOrder().getId())
                        .with(csrf())
                        .param("riderName", "Kuya Nonoy")
                        .param("riderPhone", "0917 444 9901")
                        .param("plateNumber", "BN-402"))
                .andExpect(status().is3xxRedirection());

        // Manually advance to DELIVERED
        mockMvc.perform(post("/admin/deliveries/{did}/status", del.getId())
                        .with(csrf())
                        .param("status", "DELIVERED"))
                .andExpect(status().is3xxRedirection());

        Delivery updated = deliveryRepository.findWithOrderById(del.getId()).orElseThrow();
        assertThat(updated.getProvider()).isEqualTo(DeliveryProvider.IN_HOUSE);
        assertThat(updated.getRiderName()).isEqualTo("Kuya Nonoy");
        assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }
}
