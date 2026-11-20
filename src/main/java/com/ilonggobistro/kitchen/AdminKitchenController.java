package com.ilonggobistro.kitchen;

import com.ilonggobistro.pos.PosOrder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/kitchen")
public class AdminKitchenController {

    private final KitchenService kitchenService;

    public AdminKitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping
    public String kitchenQueuePage(Model model) {
        model.addAttribute("board", kitchenService.getBoardSnapshot());
        return "admin/kitchen";
    }

    @GetMapping("/board")
    public String kitchenBoardFragment(Model model) {
        model.addAttribute("board", kitchenService.getBoardSnapshot());
        return "admin/fragments/kitchen-board :: board";
    }

    @PostMapping("/{id}/status")
    public String updateTicketStatus(
            @PathVariable Long id,
            @RequestParam("status") KitchenStatus status,
            Principal principal,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        String staffName = principal != null ? principal.getName() : "kitchen";
        try {
            PosOrder updated = kitchenService.updateKitchenStatus(id, status, staffName);
            String feedback = "Ticket " + updated.getReceiptNumber() + " moved to " + status.getStaffLabel() + ".";

            if ("true".equalsIgnoreCase(hxRequest)) {
                model.addAttribute("board", kitchenService.getBoardSnapshot());
                model.addAttribute("kdsFeedback", feedback);
                return "admin/fragments/kitchen-board :: board";
            }
            redirectAttributes.addFlashAttribute("flashSuccess", feedback);
        } catch (IllegalArgumentException ex) {
            if ("true".equalsIgnoreCase(hxRequest)) {
                model.addAttribute("board", kitchenService.getBoardSnapshot());
                model.addAttribute("kdsError", ex.getMessage());
                return "admin/fragments/kitchen-board :: board";
            }
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/kitchen";
    }

    @GetMapping("/{id}/kot")
    public String printKitchenOrderTicket(
            @PathVariable Long id,
            @RequestParam(name = "width", defaultValue = "80") int width,
            @RequestParam(name = "autoprint", defaultValue = "false") boolean autoPrint,
            Model model) {
        PosOrder order = kitchenService.getOrderForKot(id);
        int paperWidthMm = (width == 58) ? 58 : 80;
        model.addAttribute("order", order);
        model.addAttribute("paperWidthMm", paperWidthMm);
        model.addAttribute("autoPrint", autoPrint);
        return "admin/kitchen-kot";
    }
}
