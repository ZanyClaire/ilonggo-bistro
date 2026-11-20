package com.ilonggobistro.kitchen;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import com.ilonggobistro.auth.StaffLoginSuccessHandler;
import com.ilonggobistro.delivery.Delivery;
import com.ilonggobistro.delivery.DeliveryProvider;
import com.ilonggobistro.delivery.DeliveryRepository;
import com.ilonggobistro.pos.OrderType;
import com.ilonggobistro.pos.PaymentMethod;
import com.ilonggobistro.pos.PosOrder;
import com.ilonggobistro.pos.PosOrderItem;
import com.ilonggobistro.pos.PosOrderRepository;
import com.ilonggobistro.pos.PosService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KitchenDisplaySystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PosOrderRepository orderRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private PosService posService;

    @Autowired
    private KitchenService kitchenService;

    @Autowired
    private StaffLoginSuccessHandler loginSuccessHandler;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    private final List<Long> createdDeliveryIds = new ArrayList<>();
    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUpTestRecords() {
        createdDeliveryIds.forEach(id -> deliveryRepository.findById(id).ifPresent(deliveryRepository::delete));
        createdDeliveryIds.clear();
        createdOrderIds.forEach(id -> orderRepository.findById(id).ifPresent(orderRepository::delete));
        createdOrderIds.clear();
        createdUserIds.forEach(id -> adminUserRepository.findById(id).ifPresent(adminUserRepository::delete));
        createdUserIds.clear();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanViewKitchenBoardAndUpdateStatus() throws Exception {
        PosOrder ticket = saveOrder("KDS-ADM-01", "Elena Cruz", OrderType.DINE_IN, "Table 4",
                LocalDateTime.now(clock).minusMinutes(5), "No spring onions",
                List.of(new ItemSpec("Signature La Paz Batchoy", 2, "195.00")));

        mockMvc.perform(get("/admin/kitchen"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kitchen Queue (KDS)")))
                .andExpect(content().string(containsString("KDS-ADM-01")));

        mockMvc.perform(post("/admin/kitchen/{id}/status", ticket.getId())
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("status", "COOKING"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cooking Now")));
    }

    @Test
    @WithMockUser(username = "kitchen_chef", roles = {"KITCHEN_STAFF"})
    void kitchenStaffCanViewBoardAndAdvanceTicket() throws Exception {
        PosOrder ticket = saveOrder("KDS-CHEF-01", "Marco Lim", OrderType.TAKEOUT, "Takeout",
                LocalDateTime.now(clock).minusMinutes(3), null,
                List.of(new ItemSpec("Chicken Inasal Pecho", 1, "235.00")));

        mockMvc.perform(get("/admin/kitchen")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/kitchen/board")).andExpect(status().isOk());

        mockMvc.perform(post("/admin/kitchen/{id}/status", ticket.getId())
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("status", "COOKING"))
                .andExpect(status().isOk());

        PosOrder updated = orderRepository.findById(ticket.getId()).orElseThrow();
        assertThat(updated.getKitchenStatus()).isEqualTo(KitchenStatus.COOKING);
        assertThat(updated.getKitchenUpdatedBy()).isEqualTo("kitchen_chef");
    }

    @Test
    @WithMockUser(username = "cashier_joy", roles = {"CASHIER"})
    void cashierCanViewKitchenBoardAndMarkHandedOff() throws Exception {
        PosOrder ticket = saveOrder("KDS-CASH-01", "Dina Reyes", OrderType.PICKUP, "Pickup",
                LocalDateTime.now(clock).minusMinutes(7), null,
                List.of(new ItemSpec("Pancit Molo", 1, "185.00")));
        kitchenService.updateKitchenStatus(ticket.getId(), KitchenStatus.READY, "kitchen");

        mockMvc.perform(get("/admin/kitchen")).andExpect(status().isOk());
        mockMvc.perform(post("/admin/kitchen/{id}/status", ticket.getId())
                        .with(csrf())
                        .param("status", "SERVED"))
                .andExpect(status().is3xxRedirection());

        PosOrder updated = orderRepository.findById(ticket.getId()).orElseThrow();
        assertThat(updated.getKitchenStatus()).isEqualTo(KitchenStatus.SERVED);
        assertThat(updated.getKitchenUpdatedBy()).isEqualTo("cashier_joy");
    }

    @Test
    @WithMockUser(username = "host_paolo", roles = {"BOOKING_HOST"})
    void bookingHostIsBlockedFromKitchenQueueWith403() throws Exception {
        PosOrder ticket = saveOrder("KDS-HOST-01", "Guest", OrderType.DINE_IN, "Table 1",
                LocalDateTime.now(clock).minusMinutes(2), null,
                List.of(new ItemSpec("Chicken Inasal", 1, "235.00")));

        mockMvc.perform(get("/admin/kitchen")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/kitchen/board")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/kitchen/{id}/kot", ticket.getId())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/kitchen/{id}/status", ticket.getId())
                        .with(csrf())
                        .param("status", "COOKING"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef_rico", roles = {"KITCHEN_STAFF"})
    void fifoOrderingFullWorkflowTransitionsAndRecallWorkCleanly() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        PosOrder newer = saveOrder("KDS-FIFO-NEW", "Newer Guest", OrderType.DINE_IN, "Table 8",
                now.minusMinutes(2), null,
                List.of(new ItemSpec("KBL Clay Pot", 1, "320.00")));
        PosOrder older = saveOrder("KDS-FIFO-OLD", "Older Guest", OrderType.DINE_IN, "Table 2",
                now.minusMinutes(18), "Extra crispy chicharon",
                List.of(new ItemSpec("Signature La Paz Batchoy", 2, "195.00")));

        KitchenService.KitchenBoardSnapshot snap1 = kitchenService.getBoardSnapshot();
        List<String> pendingReceipts = snap1.pendingOrders().stream().map(PosOrder::getReceiptNumber).toList();
        assertThat(pendingReceipts.indexOf("KDS-FIFO-OLD"))
                .isLessThan(pendingReceipts.indexOf("KDS-FIFO-NEW"));

        // Verify board HTML renders NEXT UP #1 badge
        mockMvc.perform(get("/admin/kitchen/board"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXT UP #1")))
                .andExpect(content().string(containsString("Extra crispy chicharon")));

        // PENDING -> COOKING
        PosOrder cooking = kitchenService.updateKitchenStatus(older.getId(), KitchenStatus.COOKING, "chef_rico");
        assertThat(cooking.getKitchenStatus()).isEqualTo(KitchenStatus.COOKING);
        assertThat(cooking.getKitchenStartedAt()).isNotNull();
        assertThat(cooking.getKitchenUpdatedBy()).isEqualTo("chef_rico");

        // COOKING -> READY
        PosOrder ready = kitchenService.updateKitchenStatus(older.getId(), KitchenStatus.READY, "chef_rico");
        assertThat(ready.getKitchenStatus()).isEqualTo(KitchenStatus.READY);
        assertThat(ready.getKitchenReadyAt()).isNotNull();

        // READY -> SERVED
        PosOrder served = kitchenService.updateKitchenStatus(older.getId(), KitchenStatus.SERVED, "chef_rico");
        assertThat(served.getKitchenStatus()).isEqualTo(KitchenStatus.SERVED);
        assertThat(served.getKitchenCompletedAt()).isNotNull();

        KitchenService.KitchenBoardSnapshot afterServed = kitchenService.getBoardSnapshot();
        assertThat(afterServed.servedTodayOrders())
                .extracting(PosOrder::getReceiptNumber)
                .contains("KDS-FIFO-OLD");

        // Recall SERVED -> READY (undo accidental bump)
        PosOrder recalled = kitchenService.updateKitchenStatus(older.getId(), KitchenStatus.READY, "chef_rico");
        assertThat(recalled.getKitchenStatus()).isEqualTo(KitchenStatus.READY);
        assertThat(recalled.getKitchenCompletedAt()).isNull();

        // Verify KOT renders items with NO prices
        mockMvc.perform(get("/admin/kitchen/{id}/kot", older.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("KITCHEN ORDER TICKET")))
                .andExpect(content().string(containsString("Signature La Paz Batchoy")))
                .andExpect(content().string(containsString("Extra crispy chicharon")))
                .andExpect(content().string(not(containsString("₱"))));
    }

    @Test
    void voidedOrdersAreExcludedFromActiveBoardAndDishPrepCounter() {
        LocalDateTime now = LocalDateTime.now(clock);
        String uniqueDish = "Special Batchoy Test " + UUID.randomUUID().toString().substring(0, 6);

        PosOrder pending1 = saveOrder("KDS-PREP-1", "Guest 1", OrderType.DINE_IN, "Table 1",
                now.minusMinutes(6), null, List.of(new ItemSpec(uniqueDish, 2, "195.00")));
        PosOrder cooking1 = saveOrder("KDS-PREP-2", "Guest 2", OrderType.TAKEOUT, "Takeout",
                now.minusMinutes(11), null, List.of(new ItemSpec(uniqueDish, 3, "195.00")));
        PosOrder ready1 = saveOrder("KDS-PREP-3", "Guest 3", OrderType.PICKUP, "Pickup",
                now.minusMinutes(14), null, List.of(new ItemSpec(uniqueDish, 4, "195.00")));
        PosOrder toVoid = saveOrder("KDS-PREP-VOID", "Guest Void", OrderType.DINE_IN, "Table 9",
                now.minusMinutes(4), null, List.of(new ItemSpec(uniqueDish, 5, "195.00")));

        kitchenService.updateKitchenStatus(cooking1.getId(), KitchenStatus.COOKING, "kitchen");
        kitchenService.updateKitchenStatus(ready1.getId(), KitchenStatus.READY, "kitchen");

        // Voiding removes it immediately from KDS active queue & prep totals
        posService.voidOrder(toVoid.getId());

        KitchenService.KitchenBoardSnapshot snap = kitchenService.getBoardSnapshot();
        assertThat(snap.pendingOrders()).extracting(PosOrder::getReceiptNumber).doesNotContain("KDS-PREP-VOID");
        assertThat(snap.cookingOrders()).extracting(PosOrder::getReceiptNumber).doesNotContain("KDS-PREP-VOID");
        assertThat(snap.readyOrders()).extracting(PosOrder::getReceiptNumber).doesNotContain("KDS-PREP-VOID");
        assertThat(snap.servedTodayOrders()).extracting(PosOrder::getReceiptNumber).doesNotContain("KDS-PREP-VOID");

        // Dish prep summary should sum ONLY pending (2) + cooking (3) = 5 (excluding ready 4 & voided 5)
        KitchenService.DishPrepCount dishCount = snap.dishPrepSummary().stream()
                .filter(d -> d.itemName().equals(uniqueDish))
                .findFirst()
                .orElseThrow();
        assertThat(dishCount.totalQuantity()).isEqualTo(5);
        assertThat(dishCount.pendingQuantity()).isEqualTo(2);
        assertThat(dishCount.cookingQuantity()).isEqualTo(3);

        // Guard: voided orders cannot be moved back on the KDS board
        assertThatThrownBy(() -> kitchenService.updateKitchenStatus(toVoid.getId(), KitchenStatus.COOKING, "kitchen"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voided");

        // Keep pending1 referenced so analyzer knows it was exercised
        assertThat(snap.pendingOrders()).extracting(PosOrder::getId).contains(pending1.getId());
    }

    @Test
    @WithMockUser(username = "cashier_test", roles = {"CASHIER"})
    void deliveriesAndCustomerTrackingReflectLiveKitchenStatus() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. Pickup customer tracking shows live kitchen progress
        PosOrder pickup = saveOrder("KDS-PICKUP-01", "Lorna Reyes", OrderType.PICKUP, "Pickup",
                now.minusMinutes(4), null, List.of(new ItemSpec("Pancit Molo", 2, "185.00")));

        mockMvc.perform(get("/orders/{ref}", pickup.getReceiptNumber()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Waiting in Kitchen")));

        kitchenService.updateKitchenStatus(pickup.getId(), KitchenStatus.COOKING, "kitchen");
        mockMvc.perform(get("/orders/{ref}/status-fragment", pickup.getReceiptNumber()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cooking Now")));

        kitchenService.updateKitchenStatus(pickup.getId(), KitchenStatus.READY, "kitchen");
        mockMvc.perform(get("/orders/{ref}/status-fragment", pickup.getReceiptNumber()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ready for Counter Pickup")));

        // 2. Delivery dispatch console & customer tracking show kitchen badge for delivery orders
        PosOrder deliveryOrder = saveOrder("KDS-DEL-01", "Arnel Tan", OrderType.DELIVERY, "Delivery",
                now.minusMinutes(6), null, List.of(new ItemSpec("Chicken Inasal", 2, "235.00")));
        Delivery delivery = new Delivery(
                deliveryOrder,
                DeliveryProvider.SIMULATOR,
                "Burgos-Osmeña St, Barotac Nuevo, Iloilo",
                10.8969, 122.7022,
                "Poblacion Ilaya, Barotac Nuevo",
                "Near plaza",
                10.8924, 122.7078,
                new BigDecimal("2.10"),
                new BigDecimal("52.00"),
                25
        );
        Delivery savedDelivery = deliveryRepository.save(delivery);
        createdDeliveryIds.add(savedDelivery.getId());

        kitchenService.updateKitchenStatus(deliveryOrder.getId(), KitchenStatus.READY, "kitchen");

        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kitchen: Ready for Rider Pickup")));

        mockMvc.perform(get("/orders/{ref}", deliveryOrder.getReceiptNumber()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kitchen: Ready for Rider Pickup")));
    }

    @Test
    void kitchenStaffLoginSuccessRedirectsDirectlyToKitchenQueueUnlessMustChangePassword() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        AdminUser kitchenNormal = new AdminUser(
                "kds.chef." + suffix,
                passwordEncoder.encode("ChefPass123!"),
                "Chef Normal",
                "kds.chef." + suffix + "@gmail.com",
                "0917 444 1001",
                "KITCHEN_STAFF",
                false
        );
        AdminUser savedNormal = adminUserRepository.save(kitchenNormal);
        createdUserIds.add(savedNormal.getId());

        MockHttpServletResponse resNormal = new MockHttpServletResponse();
        loginSuccessHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                resNormal,
                new UsernamePasswordAuthenticationToken(
                        savedNormal.getUsername(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_KITCHEN_STAFF")))
        );
        assertThat(resNormal.getRedirectedUrl()).isEqualTo("/admin/kitchen");

        AdminUser kitchenMustChange = new AdminUser(
                "kds.newchef." + suffix,
                passwordEncoder.encode("TempPass123!"),
                "Chef Must Change",
                "kds.newchef." + suffix + "@gmail.com",
                "0917 444 1002",
                "KITCHEN_STAFF",
                true
        );
        AdminUser savedMustChange = adminUserRepository.save(kitchenMustChange);
        createdUserIds.add(savedMustChange.getId());

        MockHttpServletResponse resMustChange = new MockHttpServletResponse();
        loginSuccessHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                resMustChange,
                new UsernamePasswordAuthenticationToken(
                        savedMustChange.getUsername(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_KITCHEN_STAFF")))
        );
        assertThat(resMustChange.getRedirectedUrl()).isEqualTo("/admin/account?mustChange=1");
    }

    private PosOrder saveOrder(String receipt, String customer, OrderType type, String tableNumber,
                               LocalDateTime createdAt, String notes, List<ItemSpec> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemSpec spec : items) {
            subtotal = subtotal.add(new BigDecimal(spec.unitPrice()).multiply(BigDecimal.valueOf(spec.qty())));
        }
        PosOrder order = new PosOrder(
                receipt, customer, "0917 111 2222",
                type, tableNumber, PaymentMethod.CASH,
                subtotal, null, BigDecimal.ZERO, subtotal,
                subtotal, BigDecimal.ZERO,
                "cashier", notes, createdAt
        );
        for (ItemSpec spec : items) {
            BigDecimal unit = new BigDecimal(spec.unitPrice());
            order.addItem(new PosOrderItem(null, spec.name(), unit, spec.qty(), unit.multiply(BigDecimal.valueOf(spec.qty()))));
        }
        PosOrder saved = orderRepository.save(order);
        createdOrderIds.add(saved.getId());
        return saved;
    }

    private record ItemSpec(String name, int qty, String unitPrice) {
    }
}
