package com.ilonggobistro.pos;

import com.ilonggobistro.shared.StoreProfile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates standard ESC/POS binary byte streams and exact-column plain-text previews
 * for 58mm (32 columns) and 80mm (48 columns) thermal receipt printers.
 */
@Component
public class EscPosReceiptFormatter {

    private static final byte[] CMD_INIT = {0x1B, 0x40};
    private static final byte[] CMD_ALIGN_LEFT = {0x1B, 0x61, 0x00};
    private static final byte[] CMD_ALIGN_CENTER = {0x1B, 0x61, 0x01};
    private static final byte[] CMD_BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] CMD_BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] CMD_SIZE_NORMAL = {0x1D, 0x21, 0x00};
    private static final byte[] CMD_SIZE_DOUBLE = {0x1D, 0x21, 0x11};
    private static final byte[] CMD_SIZE_TALL = {0x1D, 0x21, 0x01};
    private static final byte[] CMD_FEED_4 = {0x1B, 0x64, 0x04};
    private static final byte[] CMD_PARTIAL_CUT = {0x1D, 0x56, 0x42, 0x00};
    public static final byte[] CMD_OPEN_CASH_DRAWER = {0x1B, 0x70, 0x00, 0x19, (byte) 0xFA};

    private static final DateTimeFormatter TEST_TIME_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    public byte[] buildOrderEscPos(PosOrder order, StoreProfile profile, String hoursRange, PrinterConfig config) {
        int cols = config.columns();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytes(out, CMD_INIT);

        // Header
        writeBytes(out, CMD_ALIGN_CENTER);
        writeBytes(out, CMD_BOLD_ON);
        writeBytes(out, CMD_SIZE_DOUBLE);
        writeLine(out, sanitizeAscii(profile.bistroName().toUpperCase(Locale.ENGLISH)));
        writeBytes(out, CMD_SIZE_NORMAL);
        writeBytes(out, CMD_BOLD_OFF);

        for (String line : wrap(sanitizeAscii(profile.addressLine()), cols)) {
            writeLine(out, line);
        }
        if (profile.plusCode() != null && !profile.plusCode().isBlank()) {
            writeLine(out, "Plus Code: " + sanitizeAscii(profile.plusCode()));
        }
        if (profile.phoneNumber() != null && !profile.phoneNumber().isBlank()) {
            writeLine(out, "Tel: " + sanitizeAscii(profile.phoneNumber()));
        }
        writeBytes(out, CMD_BOLD_ON);
        writeLine(out, order.getStatus() == PosOrderStatus.VOIDED ? "*** VOIDED RECEIPT ***" : "ORDER RECEIPT - PAID");
        writeBytes(out, CMD_BOLD_OFF);

        // Metadata
        writeBytes(out, CMD_ALIGN_LEFT);
        writeLine(out, divider(cols));
        writeLine(out, twoCol("Receipt #:", order.getReceiptNumber(), cols));
        if (order.getOfflineReceiptNumber() != null && !order.getOfflineReceiptNumber().isBlank()) {
            writeLine(out, twoCol("Offline Ref:", order.getOfflineReceiptNumber(), cols));
        }
        writeLine(out, twoCol("Date:", sanitizeAscii(order.formattedCreatedAt()), cols));
        writeLine(out, twoCol("Customer:", sanitizeAscii(order.getCustomerName()), cols));
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
            writeLine(out, twoCol("Phone:", sanitizeAscii(order.getCustomerPhone()), cols));
        }
        String service = order.getOrderType().getLabel()
                + (order.getTableNumber() != null && !order.getTableNumber().isBlank() ? " / " + order.getTableNumber() : "");
        writeLine(out, twoCol("Service:", sanitizeAscii(service), cols));
        writeLine(out, twoCol("Cashier:", sanitizeAscii(order.getCashierUsername()), cols));
        writeLine(out, divider(cols));

        // Items
        writeBytes(out, CMD_BOLD_ON);
        writeLine(out, twoCol("QTY ITEM", "AMOUNT", cols));
        writeBytes(out, CMD_BOLD_OFF);
        for (PosOrderItem item : order.getItems()) {
            String left = item.getQuantity() + "x " + sanitizeAscii(item.getItemName());
            String right = formatAsciiPhp(item.getLineTotalPhp());
            for (String row : formatItemRow(left, right, cols)) {
                writeLine(out, row);
            }
            if (item.getQuantity() > 1) {
                writeLine(out, "   @ " + formatAsciiPhp(item.getUnitPricePhp()) + " each");
            }
        }
        writeLine(out, divider(cols));

        // Totals
        writeLine(out, twoCol("Subtotal (" + order.totalItemCount() + " item/s)", formatAsciiPhp(order.getSubtotalPhp()), cols));
        if (order.getDiscountPhp() != null && order.getDiscountPhp().signum() > 0) {
            String discLabel = order.getDiscountLabel() != null ? sanitizeAscii(order.getDiscountLabel()) : "Discount";
            writeLine(out, twoCol(discLabel, "-" + formatAsciiPhp(order.getDiscountPhp()), cols));
        }
        if (order.getDeliveryFeePhp() != null && order.getDeliveryFeePhp().signum() > 0) {
            writeLine(out, twoCol("Delivery Fee", formatAsciiPhp(order.getDeliveryFeePhp()), cols));
        }

        writeBytes(out, CMD_BOLD_ON);
        writeBytes(out, CMD_SIZE_TALL);
        writeLine(out, twoCol("TOTAL", formatAsciiPhp(order.getTotalPhp()), cols));
        writeBytes(out, CMD_SIZE_NORMAL);
        writeBytes(out, CMD_BOLD_OFF);

        writeLine(out, twoCol("Paid (" + order.getPaymentMethod().getLabel() + ")", formatAsciiPhp(order.getAmountTenderedPhp()), cols));
        writeBytes(out, CMD_BOLD_ON);
        writeLine(out, twoCol("CHANGE", formatAsciiPhp(order.getChangePhp()), cols));
        writeBytes(out, CMD_BOLD_OFF);

        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            writeLine(out, divider(cols));
            for (String noteLine : wrap("Note: " + sanitizeAscii(order.getNotes()), cols)) {
                writeLine(out, noteLine);
            }
        }

        // Footer
        writeLine(out, divider(cols));
        writeBytes(out, CMD_ALIGN_CENTER);
        String footer = (config.footerNote() != null && !config.footerNote().isBlank())
                ? config.footerNote()
                : "Madamo gid nga salamat! Please come again.";
        for (String line : wrap(sanitizeAscii(footer), cols)) {
            writeLine(out, line);
        }
        if (hoursRange != null && !hoursRange.isBlank()) {
            writeLine(out, "Open Daily: " + sanitizeAscii(hoursRange));
        }

        writeBytes(out, CMD_FEED_4);
        if (config.openCashDrawer()) {
            writeBytes(out, CMD_OPEN_CASH_DRAWER);
        }
        if (config.autoCut()) {
            writeBytes(out, CMD_PARTIAL_CUT);
        }
        return out.toByteArray();
    }

    public String formatOrderPlainText(PosOrder order, StoreProfile profile, String hoursRange, PrinterConfig config) {
        int cols = config.columns();
        StringBuilder sb = new StringBuilder();
        sb.append(center(sanitizeAscii(profile.bistroName().toUpperCase(Locale.ENGLISH)), cols)).append('\n');
        for (String line : wrap(sanitizeAscii(profile.addressLine()), cols)) {
            sb.append(center(line, cols)).append('\n');
        }
        if (profile.phoneNumber() != null && !profile.phoneNumber().isBlank()) {
            sb.append(center("Tel: " + sanitizeAscii(profile.phoneNumber()), cols)).append('\n');
        }
        sb.append(center(order.getStatus() == PosOrderStatus.VOIDED ? "*** VOIDED RECEIPT ***" : "ORDER RECEIPT - PAID", cols)).append('\n');
        sb.append(divider(cols)).append('\n');
        sb.append(twoCol("Receipt #:", order.getReceiptNumber(), cols)).append('\n');
        if (order.getOfflineReceiptNumber() != null && !order.getOfflineReceiptNumber().isBlank()) {
            sb.append(twoCol("Offline Ref:", order.getOfflineReceiptNumber(), cols)).append('\n');
        }
        sb.append(twoCol("Date:", sanitizeAscii(order.formattedCreatedAt()), cols)).append('\n');
        sb.append(twoCol("Customer:", sanitizeAscii(order.getCustomerName()), cols)).append('\n');
        String service = order.getOrderType().getLabel()
                + (order.getTableNumber() != null && !order.getTableNumber().isBlank() ? " / " + order.getTableNumber() : "");
        sb.append(twoCol("Service:", sanitizeAscii(service), cols)).append('\n');
        sb.append(twoCol("Cashier:", sanitizeAscii(order.getCashierUsername()), cols)).append('\n');
        sb.append(divider(cols)).append('\n');
        sb.append(twoCol("QTY ITEM", "AMOUNT", cols)).append('\n');
        for (PosOrderItem item : order.getItems()) {
            String left = item.getQuantity() + "x " + sanitizeAscii(item.getItemName());
            String right = formatAsciiPhp(item.getLineTotalPhp());
            for (String row : formatItemRow(left, right, cols)) {
                sb.append(row).append('\n');
            }
            if (item.getQuantity() > 1) {
                sb.append("   @ ").append(formatAsciiPhp(item.getUnitPricePhp())).append(" each\n");
            }
        }
        sb.append(divider(cols)).append('\n');
        sb.append(twoCol("Subtotal (" + order.totalItemCount() + " item/s)", formatAsciiPhp(order.getSubtotalPhp()), cols)).append('\n');
        if (order.getDiscountPhp() != null && order.getDiscountPhp().signum() > 0) {
            String discLabel = order.getDiscountLabel() != null ? sanitizeAscii(order.getDiscountLabel()) : "Discount";
            sb.append(twoCol(discLabel, "-" + formatAsciiPhp(order.getDiscountPhp()), cols)).append('\n');
        }
        if (order.getDeliveryFeePhp() != null && order.getDeliveryFeePhp().signum() > 0) {
            sb.append(twoCol("Delivery Fee", formatAsciiPhp(order.getDeliveryFeePhp()), cols)).append('\n');
        }
        sb.append(twoCol("TOTAL", formatAsciiPhp(order.getTotalPhp()), cols)).append('\n');
        sb.append(twoCol("Paid (" + order.getPaymentMethod().getLabel() + ")", formatAsciiPhp(order.getAmountTenderedPhp()), cols)).append('\n');
        sb.append(twoCol("CHANGE", formatAsciiPhp(order.getChangePhp()), cols)).append('\n');
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            sb.append(divider(cols)).append('\n');
            for (String line : wrap("Note: " + sanitizeAscii(order.getNotes()), cols)) {
                sb.append(line).append('\n');
            }
        }
        sb.append(divider(cols)).append('\n');
        String footer = (config.footerNote() != null && !config.footerNote().isBlank())
                ? config.footerNote()
                : "Madamo gid nga salamat! Please come again.";
        for (String line : wrap(sanitizeAscii(footer), cols)) {
            sb.append(center(line, cols)).append('\n');
        }
        if (hoursRange != null && !hoursRange.isBlank()) {
            sb.append(center("Open Daily: " + sanitizeAscii(hoursRange), cols)).append('\n');
        }
        return sb.toString();
    }

    public byte[] buildTestSlipEscPos(StoreProfile profile, PrinterConfig config, LocalDateTime now) {
        int cols = config.columns();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytes(out, CMD_INIT);
        writeBytes(out, CMD_ALIGN_CENTER);
        writeBytes(out, CMD_BOLD_ON);
        writeBytes(out, CMD_SIZE_DOUBLE);
        writeLine(out, sanitizeAscii(profile.bistroName().toUpperCase(Locale.ENGLISH)));
        writeBytes(out, CMD_SIZE_NORMAL);
        writeLine(out, "RECEIPT PRINTER TEST SLIP");
        writeBytes(out, CMD_BOLD_OFF);
        writeBytes(out, CMD_ALIGN_LEFT);
        writeLine(out, divider(cols));
        writeLine(out, twoCol("Paper Width:", config.paperWidthMm() + "mm (" + cols + " cols)", cols));
        writeLine(out, twoCol("Mode:", config.mode().name(), cols));
        writeLine(out, twoCol("Auto-Cut:", config.autoCut() ? "Enabled" : "Disabled", cols));
        writeLine(out, twoCol("Printed:", now.format(TEST_TIME_FMT), cols));
        writeLine(out, divider(cols));
        writeLine(out, ruler(cols));
        writeLine(out, twoCol("1x Signature Batchoy", "PHP 195.00", cols));
        writeLine(out, twoCol("TOTAL", "PHP 195.00", cols));
        writeLine(out, divider(cols));
        writeBytes(out, CMD_ALIGN_CENTER);
        writeLine(out, "Printer connection OK!");
        writeBytes(out, CMD_FEED_4);
        if (config.autoCut()) {
            writeBytes(out, CMD_PARTIAL_CUT);
        }
        return out.toByteArray();
    }

    public String formatTestSlipPlainText(StoreProfile profile, PrinterConfig config, LocalDateTime now) {
        int cols = config.columns();
        return String.join("\n",
                center(sanitizeAscii(profile.bistroName().toUpperCase(Locale.ENGLISH)), cols),
                center("RECEIPT PRINTER TEST SLIP", cols),
                divider(cols),
                twoCol("Paper Width:", config.paperWidthMm() + "mm (" + cols + " cols)", cols),
                twoCol("Mode:", config.mode().name(), cols),
                twoCol("Auto-Cut:", config.autoCut() ? "Enabled" : "Disabled", cols),
                twoCol("Printed:", now.format(TEST_TIME_FMT), cols),
                divider(cols),
                ruler(cols),
                twoCol("1x Signature Batchoy", "PHP 195.00", cols),
                twoCol("TOTAL", "PHP 195.00", cols),
                divider(cols),
                center("Printer connection OK!", cols)
        );
    }

    public static String formatAsciiPhp(BigDecimal amount) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "PHP " + nf.format(value);
    }

    public static String sanitizeAscii(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("₱", "PHP ")
                .replace("·", "-")
                .replace("–", "-")
                .replace("—", "-")
                .replace("×", "x");
        return normalized.replaceAll("[^\\x20-\\x7E]", "");
    }

    private List<String> formatItemRow(String left, String right, int cols) {
        List<String> lines = new ArrayList<>();
        if (left.length() + 1 + right.length() <= cols) {
            lines.add(twoCol(left, right, cols));
            return lines;
        }
        int maxLeft = Math.max(10, cols - right.length() - 1);
        List<String> wrapped = wrap(left, maxLeft);
        for (int i = 0; i < wrapped.size(); i++) {
            if (i == 0) {
                lines.add(twoCol(wrapped.get(i), right, cols));
            } else {
                lines.add("  " + wrapped.get(i));
            }
        }
        return lines;
    }

    private String twoCol(String left, String right, int cols) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        int space = cols - l.length() - r.length();
        if (space >= 1) {
            return l + " ".repeat(space) + r;
        }
        int maxL = Math.max(1, cols - r.length() - 1);
        String trimmedL = l.length() > maxL ? l.substring(0, maxL) : l;
        int pad = Math.max(1, cols - trimmedL.length() - r.length());
        return trimmedL + " ".repeat(pad) + r;
    }

    private String center(String text, int cols) {
        String t = text == null ? "" : text;
        if (t.length() >= cols) return t.substring(0, cols);
        int leftPad = (cols - t.length()) / 2;
        return " ".repeat(leftPad) + t;
    }

    private String divider(int cols) {
        return "-".repeat(cols);
    }

    private String ruler(int cols) {
        StringBuilder sb = new StringBuilder(cols);
        for (int i = 1; i <= cols; i++) {
            sb.append(i % 10);
        }
        return sb.toString();
    }

    private List<String> wrap(String text, int maxCols) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.isEmpty()) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxCols) {
                current.append(' ').append(word);
            } else {
                result.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    private void writeBytes(ByteArrayOutputStream out, byte[] data) {
        out.write(data, 0, data.length);
    }

    private void writeLine(ByteArrayOutputStream out, String line) {
        byte[] bytes = (line + "\n").getBytes(StandardCharsets.US_ASCII);
        out.write(bytes, 0, bytes.length);
    }
}
