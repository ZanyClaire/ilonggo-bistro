package com.ilonggobistro.pos;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/pos")
public class AdminPrinterController {

    private final ReceiptPrinterService printerService;
    private final PosService posService;

    public AdminPrinterController(ReceiptPrinterService printerService, PosService posService) {
        this.printerService = printerService;
        this.posService = posService;
    }

    @GetMapping("/printer")
    public String printerSetupPage(Model model) {
        model.addAttribute("printerConfig", printerService.getConfig());
        model.addAttribute("printerModes", PrinterMode.values());
        model.addAttribute("systemPrinters", printerService.listInstalledSystemPrinters());
        model.addAttribute("testSlipPreview", printerService.previewTestSlipText());
        return "admin/printer-settings";
    }

    @PostMapping("/printer")
    public String savePrinterSetup(
            @RequestParam("mode") PrinterMode mode,
            @RequestParam(name = "printerName", required = false) String printerName,
            @RequestParam(name = "networkHost", required = false) String networkHost,
            @RequestParam(name = "networkPort", defaultValue = "9100") int networkPort,
            @RequestParam(name = "paperWidthMm", defaultValue = "80") int paperWidthMm,
            @RequestParam(name = "autoCut", defaultValue = "false") boolean autoCut,
            @RequestParam(name = "openCashDrawer", defaultValue = "false") boolean openCashDrawer,
            @RequestParam(name = "autoPrintAfterCheckout", defaultValue = "false") boolean autoPrintAfterCheckout,
            @RequestParam(name = "footerNote", required = false) String footerNote,
            RedirectAttributes redirectAttributes) {
        printerService.saveConfig(mode, printerName, networkHost, networkPort, paperWidthMm,
                autoCut, openCashDrawer, autoPrintAfterCheckout, footerNote);
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Receipt printer settings saved (" + printerService.getConfig().targetSummary() + ").");
        return "redirect:/admin/pos/printer";
    }

    @PostMapping("/printer/test")
    public String printTestSlip(RedirectAttributes redirectAttributes) {
        ReceiptPrinterService.PrintResult result = printerService.printTestSlipToHardware();
        if (result.success()) {
            redirectAttributes.addFlashAttribute("flashSuccess", result.message());
        } else {
            redirectAttributes.addFlashAttribute("flashError", result.message());
        }
        return "redirect:/admin/pos/printer";
    }

    @PostMapping("/printer/open-drawer")
    public String openDrawer(RedirectAttributes redirectAttributes) {
        ReceiptPrinterService.PrintResult result = printerService.openCashDrawer();
        if (result.success()) {
            redirectAttributes.addFlashAttribute("flashSuccess", result.message());
        } else {
            redirectAttributes.addFlashAttribute("flashError", result.message());
        }
        return "redirect:/admin/pos/printer";
    }

    @PostMapping("/receipt/{receiptNumber}/print-escpos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> directPrintReceipt(@PathVariable String receiptNumber) {
        ReceiptPrinterService.PrintResult result = printerService.printOrderToHardware(receiptNumber);
        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.message(),
                    "target", result.target()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", result.message(),
                "target", result.target()
        ));
    }

    @GetMapping("/receipt/{receiptNumber}/thermal")
    public String dedicatedThermalView(
            @PathVariable String receiptNumber,
            @RequestParam(name = "width", required = false) Integer widthMm,
            @RequestParam(name = "autoprint", defaultValue = "0") boolean autoPrint,
            Model model) {
        PrinterConfig config = printerService.getConfig();
        int effectiveWidth = (widthMm != null && widthMm <= 58) ? 58
                : (widthMm != null ? 80 : config.paperWidthMm());
        model.addAttribute("order", posService.getByReceiptNumber(receiptNumber));
        model.addAttribute("paperWidthMm", effectiveWidth);
        model.addAttribute("autoPrint", autoPrint);
        model.addAttribute("footerNote", config.footerNote());
        return "admin/pos-receipt-thermal";
    }
}
