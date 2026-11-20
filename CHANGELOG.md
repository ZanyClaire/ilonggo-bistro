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

## [v1.0.0] - 2026-11-20
### Baseline Release
- **Public Bistro Website:** Home hero, live `Asia/Manila` Open/Closed status, interactive HTMX Digital Menu (`/menu`), Dine-in Table Reservations (`/reservations`), Gallery (`/gallery`), About & Directions (`Burgos-Osmeña St, Barotac Nuevo, Iloilo · VPW2+QF`), Contact (`/contact`), official Est. 2025 logo & clean single-row navbar.
- **POS Terminal, Printable Receipts & Buyer History:** Counter POS (`/admin/pos`) with Senior/PWD 20% & custom discounts, Cash/GCash/Maya/Card, 80mm thermal printable receipts (`/admin/pos/receipt/{ref}`), searchable customer purchase history (`/admin/sales`), and Menu Category CRUD (`/admin/categories`).
- **Business Events & Multi-Photo Albums:** Event hubs & albums (`/events`, `/admin/events`) supporting batch uploads (up to 20 photos at once) for Awards Night, Dining Events, and Catering, with guest photo moderation.
- **On-Demand Rider Delivery Dispatch:** Leaflet/OpenStreetMap checkout map pin (`/order`), `POST /api/delivery/quote`, `pandago` & `GrabExpress` adapters + zero-key Sandbox Simulator + In-House Local Rider fallback, `POST /api/webhooks/delivery`, Cashier Dispatch Console (`/admin/deliveries`), and live HTMX customer tracking (`/orders/{ref}`).
- **Role-Based Staff Management, OTP & Google Sign-In:** 4-role RBAC (`ADMIN`, `CASHIER`, `BOOKING_HOST`, `KITCHEN_STAFF`) with custom `403` page, Staff Directory (`/admin/staff`) with WhatsApp quick-chat links, Account Security (`/admin/account`) & Forgot Password (`/admin/forgot-password`) via 6-digit Gmail/WhatsApp OTP (SMTP & Meta Cloud adapters + zero-key Simulator), and Sign in with Google OAuth2 + Dev Simulator.
- **Automated Verification:** 26 JUnit 5 + Spring MockMvc tests passing (`V1`–`V6` Flyway migrations).
