package com.ilonggobistro.pos;

import com.ilonggobistro.shared.BusinessHoursService;
import com.ilonggobistro.shared.StoreProfile;
import com.ilonggobistro.shared.StoreSetting;
import com.ilonggobistro.shared.StoreSettingRepository;
import com.ilonggobistro.shared.StoreSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReceiptPrinterService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptPrinterService.class);
    private static final int NETWORK_TIMEOUT_MS = 3500;

    private final StoreSettingRepository settingRepository;
    private final StoreSettingsService storeSettingsService;
    private final BusinessHoursService businessHoursService;
    private final PosService posService;
    private final EscPosReceiptFormatter formatter;
    private final Clock clock;

    public ReceiptPrinterService(StoreSettingRepository settingRepository,
                                 StoreSettingsService storeSettingsService,
                                 BusinessHoursService businessHoursService,
                                 PosService posService,
                                 EscPosReceiptFormatter formatter,
                                 Clock clock) {
        this.settingRepository = settingRepository;
        this.storeSettingsService = storeSettingsService;
        this.businessHoursService = businessHoursService;
        this.posService = posService;
        this.formatter = formatter;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PrinterConfig getConfig() {
        Map<String, String> map = settingRepository.findAll().stream()
                .collect(Collectors.toMap(StoreSetting::getKey, StoreSetting::getValue, (a, b) -> b));

        return new PrinterConfig(
                PrinterMode.fromStringOrDefault(map.get("printer_mode")),
                map.getOrDefault("printer_name", ""),
                map.getOrDefault("printer_network_host", ""),
                parseInt(map.get("printer_network_port"), 9100),
                parseInt(map.get("printer_paper_width_mm"), 80),
                parseBool(map.get("printer_auto_cut"), true),
                parseBool(map.get("printer_open_drawer"), false),
                parseBool(map.get("printer_auto_print"), false),
                map.getOrDefault("printer_footer_note", "Madamo gid nga salamat! Please come again.")
        );
    }

    @Transactional
    public void saveConfig(PrinterMode mode, String printerName, String networkHost,
                           int networkPort, int paperWidthMm, boolean autoCut,
                           boolean openCashDrawer, boolean autoPrintAfterCheckout, String footerNote) {
        int safeWidth = paperWidthMm <= 58 ? 58 : 80;
        int safePort = (networkPort < 1 || networkPort > 65535) ? 9100 : networkPort;

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("printer_mode", mode.name());
        updates.put("printer_name", printerName != null ? printerName.trim() : "");
        updates.put("printer_network_host", networkHost != null ? networkHost.trim() : "");
        updates.put("printer_network_port", String.valueOf(safePort));
        updates.put("printer_paper_width_mm", String.valueOf(safeWidth));
        updates.put("printer_auto_cut", String.valueOf(autoCut));
        updates.put("printer_open_drawer", String.valueOf(openCashDrawer));
        updates.put("printer_auto_print", String.valueOf(autoPrintAfterCheckout));
        updates.put("printer_footer_note", footerNote != null ? footerNote.trim() : "");
        storeSettingsService.updateSettings(updates);
    }

    public List<String> listInstalledSystemPrinters() {
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services == null) {
                return List.of();
            }
            return Arrays.stream(services)
                    .map(PrintService::getName)
                    .sorted(String::compareToIgnoreCase)
                    .toList();
        } catch (Exception ex) {
            log.warn("Could not enumerate OS print services: {}", ex.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public String previewReceiptText(String receiptNumber) {
        PosOrder order = posService.getByReceiptNumber(receiptNumber);
        StoreProfile profile = storeSettingsService.getProfile();
        String hoursRange = businessHoursService.getCurrentStatus().hoursRange();
        return formatter.formatOrderPlainText(order, profile, hoursRange, getConfig());
    }

    @Transactional(readOnly = true)
    public String previewTestSlipText() {
        StoreProfile profile = storeSettingsService.getProfile();
        return formatter.formatTestSlipPlainText(profile, getConfig(), LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public PrintResult printOrderToHardware(String receiptNumber) {
        PrinterConfig config = getConfig();
        if (!config.isDirectEscPosMode()) {
            return new PrintResult(false,
                    "Printer mode is set to Browser Print. Switch to Windows USB or Network LAN in Receipt Printer Setup — or click Print 80mm / 58mm.",
                    config.targetSummary());
        }
        PosOrder order = posService.getByReceiptNumber(receiptNumber);
        StoreProfile profile = storeSettingsService.getProfile();
        String hoursRange = businessHoursService.getCurrentStatus().hoursRange();
        byte[] payload = formatter.buildOrderEscPos(order, profile, hoursRange, config);
        return dispatchRawEscPos(payload, config, "Receipt " + order.getReceiptNumber());
    }

    @Transactional(readOnly = true)
    public PrintResult printTestSlipToHardware() {
        PrinterConfig config = getConfig();
        if (!config.isDirectEscPosMode()) {
            return new PrintResult(false,
                    "Select Windows USB Printer or Network LAN Printer mode and Save first to run a direct ESC/POS test print.",
                    config.targetSummary());
        }
        StoreProfile profile = storeSettingsService.getProfile();
        byte[] payload = formatter.buildTestSlipEscPos(profile, config, LocalDateTime.now(clock));
        return dispatchRawEscPos(payload, config, "Test Slip");
    }

    @Transactional(readOnly = true)
    public PrintResult openCashDrawer() {
        PrinterConfig config = getConfig();
        if (!config.isDirectEscPosMode()) {
            return new PrintResult(false,
                    "Cash drawer kick requires Direct ESC/POS mode (Windows USB or Network LAN receipt printer).",
                    config.targetSummary());
        }
        return dispatchRawEscPos(EscPosReceiptFormatter.CMD_OPEN_CASH_DRAWER, config, "Cash Drawer Pulse");
    }

    private PrintResult dispatchRawEscPos(byte[] rawBytes, PrinterConfig config, String jobLabel) {
        try {
            if (config.mode() == PrinterMode.WINDOWS_USB) {
                sendToOsPrinter(rawBytes, config.printerName());
            } else if (config.mode() == PrinterMode.NETWORK_TCP) {
                sendToNetworkPrinter(rawBytes, config.networkHost(), config.networkPort());
            }
            log.info("Dispatched {} ({} bytes) to {}", jobLabel, rawBytes.length, config.targetSummary());
            return new PrintResult(true, jobLabel + " sent to " + config.targetSummary() + "!", config.targetSummary());
        } catch (Exception ex) {
            log.warn("Receipt printer dispatch failed for {}: {}", jobLabel, ex.getMessage());
            return new PrintResult(false, "Could not print to " + config.targetSummary() + ": " + ex.getMessage(), config.targetSummary());
        }
    }

    private void sendToOsPrinter(byte[] rawBytes, String printerName) throws PrintException {
        if (printerName == null || printerName.isBlank()) {
            throw new IllegalArgumentException("No Windows / USB printer is selected in Printer Setup.");
        }
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService matched = Arrays.stream(services)
                .filter(s -> s.getName().equalsIgnoreCase(printerName.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Printer \"" + printerName + "\" was not found. Make sure it is powered on and connected."));

        DocPrintJob job = matched.createPrintJob();
        Doc doc = new SimpleDoc(rawBytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        job.print(doc, null);
    }

    private void sendToNetworkPrinter(byte[] rawBytes, String host, int port) throws IOException {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Network printer IP address is empty.");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host.trim(), port), NETWORK_TIMEOUT_MS);
            socket.setSoTimeout(NETWORK_TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            out.write(rawBytes);
            out.flush();
        }
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private boolean parseBool(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return Boolean.parseBoolean(raw.trim());
    }

    public record PrintResult(boolean success, String message, String target) {
    }
}
