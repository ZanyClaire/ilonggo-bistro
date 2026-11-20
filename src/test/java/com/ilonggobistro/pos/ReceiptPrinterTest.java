package com.ilonggobistro.pos;

import com.ilonggobistro.menu.MenuItem;
import com.ilonggobistro.menu.MenuService;
import com.ilonggobistro.shared.StoreProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReceiptPrinterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EscPosReceiptFormatter formatter;

    @Autowired
    private ReceiptPrinterService printerService;

    @Autowired
    private PosService posService;

    @Autowired
    private MenuService menuService;

    @Test
    void formatsEscPosBytesAndExactColumnWidthsFor58mmAnd80mm() {
        StoreProfile profile = new StoreProfile(
                "Ilonggo Bistro", "Tagline", "Burgos-Osmeña St, Barotac Nuevo, 5007 Iloilo",
                "VPW2+QF", "https://maps.google.com", LocalTime.of(7, 0), LocalTime.of(21, 0),
                "+63 917 800 2025", "hello@ilonggobistro.ph", "", "", "", 20, 40
        );
        PosOrder order = new PosOrder(
                "OR-2026-PRT01", "Juan Dela Cruz", "0917 111 2222",
                OrderType.DINE_IN, "Table 4", PaymentMethod.CASH,
                new BigDecimal("390.00"), null, BigDecimal.ZERO, new BigDecimal("390.00"),
                new BigDecimal("500.00"), new BigDecimal("110.00"),
                "cashier", "Extra garlic", LocalDateTime.of(2026, 11, 20, 11, 5)
        );
        order.addItem(new PosOrderItem(1L, "Signature La Paz Batchoy", new BigDecimal("195.00"), 2, new BigDecimal("390.00")));

        PrinterConfig cfg58 = new PrinterConfig(PrinterMode.WINDOWS_USB, "POS-58", "", 9100, 58, true, true, false, "Salamat!");
        String text58 = formatter.formatOrderPlainText(order, profile, "7 AM – 9 PM", cfg58);
        assertThat(text58).contains("ILONGGO BISTRO").contains("Osmena St").contains("OR-2026-PRT01").contains("PHP 390.00").contains("PHP 110.00");
        assertThat(text58.lines()).allSatisfy(line -> assertThat(line.length()).isLessThanOrEqualTo(32));

        byte[] rawBytes = formatter.buildOrderEscPos(order, profile, "7 AM – 9 PM", cfg58);
        // Starts with ESC @ (0x1B 0x40) and ends with partial cut GS V 66 0 (0x1D 0x56 0x42 0x00)
        assertThat(rawBytes[0]).isEqualTo((byte) 0x1B);
        assertThat(rawBytes[1]).isEqualTo((byte) 0x40);
        assertThat(rawBytes[rawBytes.length - 4]).isEqualTo((byte) 0x1D);
        assertThat(rawBytes[rawBytes.length - 3]).isEqualTo((byte) 0x56);
        assertThat(rawBytes[rawBytes.length - 2]).isEqualTo((byte) 0x42);
        assertThat(rawBytes[rawBytes.length - 1]).isEqualTo((byte) 0x00);
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void cashierCanConfigurePrinterAndRender58mmAnd80mmThermalReceipts() throws Exception {
        mockMvc.perform(get("/admin/pos/printer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thermal Receipt Printer")));

        mockMvc.perform(post("/admin/pos/printer")
                        .with(csrf())
                        .param("mode", "BROWSER")
                        .param("paperWidthMm", "58")
                        .param("autoCut", "true")
                        .param("openCashDrawer", "false")
                        .param("autoPrintAfterCheckout", "false")
                        .param("footerNote", "Madamo gid nga salamat!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pos/printer"));

        assertThat(printerService.getConfig().paperWidthMm()).isEqualTo(58);

        MenuItem item = menuService.listAllItems().getFirst();
        PosOrder saved = posService.checkout(new PosCheckoutRequest(
                "Ana Morales", null, OrderType.DINE_IN, "Table 2",
                PaymentMethod.CASH, "NONE", BigDecimal.ZERO, new BigDecimal("500.00"),
                null, List.of(new PosCheckoutRequest.CartLine(item.getId(), 1))
        ), "cashier");

        // Dedicated 58mm & 80mm thermal views render cleanly
        mockMvc.perform(get("/admin/pos/receipt/{ref}/thermal", saved.getReceiptNumber()).param("width", "58"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("size: 58mm auto")))
                .andExpect(content().string(containsString(saved.getReceiptNumber())));

        mockMvc.perform(get("/admin/pos/receipt/{ref}/thermal", saved.getReceiptNumber()).param("width", "80"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("size: 80mm auto")));

        // Standard receipt page includes Print 80mm, Print 58mm & ESC/POS preview
        mockMvc.perform(get("/admin/pos/receipt/{ref}", saved.getReceiptNumber()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Print 80mm")))
                .andExpect(content().string(containsString("Print 58mm")));

        // Direct ESC/POS endpoint returns clear JSON message when in BROWSER mode
        mockMvc.perform(post("/admin/pos/receipt/{ref}/print-escpos", saved.getReceiptNumber()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "kitchen", roles = {"KITCHEN_STAFF"})
    void kitchenStaffCannotAccessReceiptPrinterSetup() throws Exception {
        mockMvc.perform(get("/admin/pos/printer")).andExpect(status().isForbidden());
    }
}
