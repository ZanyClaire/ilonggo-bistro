# Changelog — Ilonggo Bistro Web Platform

All notable versions of **Ilonggo Bistro** are saved as annotated Git tags (`vMAJOR.MINOR.PATCH`) on GitHub so you can always inspect or roll back to any previous working version before applying new modifications.

## Versioning Policy
- **Before modifying the system:** Save and tag the current working version (`.\scripts\save-version.ps1 -Version v1.0.1 -Message "Snapshot before ..."`), or create a feature branch (`git switch -c feature/...`) off the latest tag.
- **After modifying & testing:** Run `.\scripts\save-version.ps1 -Version v1.1.0 -Message "Summary of new feature"` to verify all tests pass, tag the new release, and push both `main` and tags to GitHub.
- **To restore any previous version safely (without erasing newer history):**
  ```powershell
  git tag -l -n                         # List all saved versions
  git switch -c restore-v1.0.0 v1.0.0   # Open a clean branch at v1.0.0
  ```

---

## [v1.2.0] - 2026-11-20
### Thermal Receipt Printer Integration (ESC/POS + 58mm / 80mm)
- **Direct Hardware Printing (`ReceiptPrinterService` + `EscPosReceiptFormatter`):**
  - Supports **Windows USB / Bluetooth Thermal Printers** via Java `javax.print` raw ESC/POS dispatch (auto-discovers installed OS printers).
  - Supports **Network LAN / Wi-Fi Thermal Printers** via raw TCP Port `9100` socket.
  - Supports both **80mm (48 columns)** and **58mm (32 columns)** thermal paper rolls with ASCII-safe PHP formatting, word wrapping, optional **auto paper cut (`GS V 66 0`)**, and optional **RJ11 cash drawer kick (`ESC p`)**.
- **Dedicated Browser Thermal Print Views (`/admin/pos/receipt/{ref}/thermal`):**
  - Zero-chrome `@page` 80mm & 58mm thermal slip templates with auto-print support for any browser, tablet, or phone.
- **Receipt Printer Setup Page (`/admin/pos/printer`):**
  - Accessible to `ADMIN` and `CASHIER`, featuring detected Windows printer picker, LAN IP/Port fields, roll width selector, custom footer note, live ESC/POS column preview, **Send Test Slip**, **Kick Cash Drawer**, and optional **Auto-Print after Checkout**.

---

## [v1.1.0] - 2026-11-20
### Added — Offline-First PWA POS & Auto-Sync (Option 1)
- **Installable POS PWA & Service Worker:** Added `manifest.webmanifest` ("Ilonggo Bistro POS") and `sw-pos.js` caching `/admin/pos`, `/admin/pos/catalog.json`, compiled CSS, JS, and dish images so cashiers can keep operating even during internet outages.
- **IndexedDB Offline Order Queue & Instant 80mm Receipts:** Added `static/js/pos-offline.js` + live Online/Offline status bar, **Test Offline Mode** toggle, automatic mid-checkout network-drop failover, instant offline printable 80mm receipts (`OR-OFF-YYYY-XXXXX`), and a local **Offline Receipts** reprint drawer.
- **Idempotent Background Auto-Sync:** Added Flyway `V7__pos_offline_sync.sql` (`client_order_uuid`, `offline_receipt_number`, `synced_from_offline`) and `POST /admin/pos/sync` + `GET /admin/pos/csrf-token` so queued offline sales automatically sync on reconnect with strict UUID deduplication and full audit traceability in `/admin/sales` and `/admin/pos/receipt/{ref}`.

---

## [v1.0.0] - 2026-11-20
### Baseline Release
- **Public Bistro Website:** Home hero, live `Asia/Manila` Open/Closed status, interactive HTMX Digital Menu (`/menu`), Dine-in Table Reservations (`/reservations`), Gallery (`/gallery`), About & Directions (`Burgos-Osmeña St, Barotac Nuevo, Iloilo · VPW2+QF`), Contact (`/contact`), official Est. 2025 logo & clean single-row navbar.
- **POS Terminal, Printable Receipts & Buyer History:** Counter POS (`/admin/pos`) with Senior/PWD 20% & custom discounts, Cash/GCash/Maya/Card, 80mm thermal printable receipts (`/admin/pos/receipt/{ref}`), searchable customer purchase history (`/admin/sales`), and Menu Category CRUD (`/admin/categories`).
- **Business Events & Multi-Photo Albums:** Event hubs & albums (`/events`, `/admin/events`) supporting batch uploads (up to 20 photos at once) for Awards Night, Dining Events, and Catering, with guest photo moderation.
- **On-Demand Rider Delivery Dispatch:** Leaflet/OpenStreetMap checkout map pin (`/order`), `POST /api/delivery/quote`, `pandago` & `GrabExpress` adapters + zero-key Sandbox Simulator + In-House Local Rider fallback, `POST /api/webhooks/delivery`, Cashier Dispatch Console (`/admin/deliveries`), and live HTMX customer tracking (`/orders/{ref}`).
- **Role-Based Staff Management, OTP & Google Sign-In:** 4-role RBAC (`ADMIN`, `CASHIER`, `BOOKING_HOST`, `KITCHEN_STAFF`) with custom `403` page, Staff Directory (`/admin/staff`) with WhatsApp quick-chat links, Account Security (`/admin/account`) & Forgot Password (`/admin/forgot-password`) via 6-digit Gmail/WhatsApp OTP (SMTP & Meta Cloud adapters + zero-key Simulator), and Sign in with Google OAuth2 + Dev Simulator.
- **Automated Verification:** 26 JUnit 5 + Spring MockMvc tests passing (`V1`–`V6` Flyway migrations).
