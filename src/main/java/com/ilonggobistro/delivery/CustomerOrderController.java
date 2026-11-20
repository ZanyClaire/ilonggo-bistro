package com.ilonggobistro.delivery;

import com.ilonggobistro.menu.MenuService;
import com.ilonggobistro.pos.PosOrder;
import com.ilonggobistro.pos.PosService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class CustomerOrderController {

    private final MenuService menuService;
    private final DeliveryService deliveryService;
    private final PosService posService;

    public CustomerOrderController(MenuService menuService,
                                   DeliveryService deliveryService,
                                   PosService posService) {
        this.menuService = menuService;
        this.deliveryService = deliveryService;
        this.posService = posService;
    }

    @GetMapping("/order")
    public String orderCheckoutPage(Model model) {
        model.addAttribute("categories", menuService.listCategories());
        model.addAttribute("menuItems", menuService.searchMenu("all", null, true));
        model.addAttribute("storeLat", deliveryService.getProperties().getStoreLat());
        model.addAttribute("storeLng", deliveryService.getProperties().getStoreLng());
        model.addAttribute("storeAddress", deliveryService.getProperties().getStoreAddress());
        model.addAttribute("activeProviderLabel", deliveryService.getProperties().resolvedProvider().getLabel());
        return "pages/order-checkout";
    }

    @PostMapping("/order/checkout")
    @ResponseBody
    public ResponseEntity<?> submitOnlineOrder(@Valid @RequestBody DeliveryDtos.OnlineOrderRequest request) {
        try {
            PosOrder saved = deliveryService.placeCustomerOrder(request);
            return ResponseEntity.ok(Map.of(
                    "receiptNumber", saved.getReceiptNumber(),
                    "redirectUrl", "/orders/" + saved.getReceiptNumber()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/orders/{receiptNumber}")
    public String orderTrackingPage(@PathVariable String receiptNumber, Model model) {
        PosOrder order = posService.getByReceiptNumber(receiptNumber);
        Delivery delivery = deliveryService.findByOrderReceipt(receiptNumber).orElse(null);
        model.addAttribute("order", order);
        model.addAttribute("delivery", delivery);
        return "pages/order-tracking";
    }

    @GetMapping("/orders/{receiptNumber}/status-fragment")
    public String orderStatusFragment(@PathVariable String receiptNumber, Model model) {
        PosOrder order = posService.getByReceiptNumber(receiptNumber);
        Delivery delivery = deliveryService.findByOrderReceipt(receiptNumber).orElse(null);
        model.addAttribute("order", order);
        model.addAttribute("delivery", delivery);
        return "fragments/delivery-status :: tracker";
    }
}
