package com.ilonggobistro.pos;

import com.ilonggobistro.menu.MenuService;
import com.ilonggobistro.shared.BusinessHoursService;
import com.ilonggobistro.shared.StoreProfile;
import com.ilonggobistro.shared.StoreSettingsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminPosController {

    private final MenuService menuService;
    private final PosService posService;
    private final StoreSettingsService storeSettingsService;
    private final BusinessHoursService businessHoursService;

    public AdminPosController(MenuService menuService,
                              PosService posService,
                              StoreSettingsService storeSettingsService,
                              BusinessHoursService businessHoursService) {
        this.menuService = menuService;
        this.posService = posService;
        this.storeSettingsService = storeSettingsService;
        this.businessHoursService = businessHoursService;
    }

    @GetMapping("/pos")
    public String posTerminal(Principal principal, Model model) {
        model.addAttribute("categories", menuService.listCategories());
        model.addAttribute("menuItems", menuService.listAllItems());
        model.addAttribute("orderTypes", OrderType.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("salesSummary", posService.getSalesSummary());
        model.addAttribute("cashierName", principal != null ? principal.getName() : "staff");
        return "admin/pos";
    }

    @GetMapping("/pos/catalog.json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> posCatalogSnapshot(Principal principal) {
        StoreProfile profile = storeSettingsService.getProfile();
        PosService.SalesSummary summary = posService.getSalesSummary();

        Map<String, Object> storeMap = new LinkedHashMap<>();
        storeMap.put("bistroName", profile.bistroName());
        storeMap.put("addressLine", profile.addressLine());
        storeMap.put("plusCode", profile.plusCode());
        storeMap.put("phoneNumber", profile.phoneNumber());
        storeMap.put("hoursRange", businessHoursService.getCurrentStatus().hoursRange());
        storeMap.put("cashierUsername", principal != null ? principal.getName() : "staff");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("store", storeMap);
        body.put("todayRevenueFormatted", summary.formattedTodayRevenue());
        body.put("todayOrdersCount", summary.todayOrders());
        body.put("categories", menuService.listCategories().stream().map(c -> Map.of(
                "id", c.getId(),
                "name", c.getName(),
                "slug", c.getSlug()
        )).toList());
        body.put("items", menuService.listAllItems().stream().map(i -> Map.of(
                "id", i.getId(),
                "name", i.getName(),
                "pricePhp", i.getPricePhp(),
                "formattedPrice", i.formattedPrice(),
                "available", i.isAvailable(),
                "categorySlug", i.getCategory().getSlug(),
                "categoryName", i.getCategory().getName(),
                "imageUrl", i.effectiveImageUrl()
        )).toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/pos/csrf-token")
    @ResponseBody
    public ResponseEntity<Map<String, String>> csrfToken(CsrfToken token) {
        if (token == null) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        ));
    }

    @PostMapping("/pos/sync")
    @ResponseBody
    public ResponseEntity<PosOfflineSyncDtos.OfflineSyncBatchResponse> syncOfflineOrders(
            @Valid @RequestBody PosOfflineSyncDtos.OfflineSyncBatchRequest request,
            Principal principal) {
        String cashier = principal != null ? principal.getName() : "staff";
        PosOfflineSyncDtos.OfflineSyncBatchResponse response = posService.syncOfflineBatch(request, cashier);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pos/checkout")
    @ResponseBody
    public ResponseEntity<?> processCheckout(
            @Valid @RequestBody PosCheckoutRequest request,
            Principal principal) {
        try {
            String cashier = principal != null ? principal.getName() : "staff";
            PosOrder saved = posService.checkout(request, cashier);
            return ResponseEntity.ok(Map.of(
                    "receiptNumber", saved.getReceiptNumber(),
                    "redirectUrl", "/admin/pos/receipt/" + saved.getReceiptNumber()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/pos/receipt/{receiptNumber}")
    public String viewReceipt(@PathVariable String receiptNumber, Model model) {
        model.addAttribute("order", posService.getByReceiptNumber(receiptNumber));
        return "admin/pos-receipt";
    }

    @GetMapping("/sales")
    public String salesHistory(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "payment", required = false, defaultValue = "ALL") String payment,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        model.addAttribute("orders", posService.searchHistory(query, payment, status, from, to));
        model.addAttribute("summary", posService.getSalesSummary());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("statuses", PosOrderStatus.values());
        model.addAttribute("searchQuery", query == null ? "" : query);
        model.addAttribute("activePayment", payment.toUpperCase());
        model.addAttribute("activeStatus", status.toUpperCase());
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        return "admin/sales";
    }

    @PostMapping("/sales/{id}/void")
    public String voidSale(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        PosOrder voided = posService.voidOrder(id);
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Receipt " + voided.getReceiptNumber() + " has been marked VOIDED.");
        return "redirect:/admin/sales";
    }
}
