# Ilonggo Bistro — System Documentation & Changelog

Welcome to the official technical and operational documentation for **Ilonggo Bistro** (*Burgos-Osmeña St, Barotac Nuevo, 5007 Iloilo · Plus Code: `VPW2+QF`*).

This document serves as the single source of truth for the system architecture, feature modules, database schemas, local development setup, environment configuration, and a living **System Update Changelog**.

> **Important Developer Guideline:**  
> Whenever an engineer or AI assistant updates, refactors, or adds new features to this project, you must append an entry to the [Changelog](#system-update-changelog) section at the bottom of this file detailing the date, version, summary of changes, and modified files.

---

## Table of Contents
1. [System Architecture & Tech Stack](#system-architecture--tech-stack)
2. [Project Structure](#project-structure)
3. [Quick Start & Running the Project](#quick-start--running-the-project)
4. [Core Modules & Features](#core-modules--features)
   - [1. Customer Website & Digital Menu](#1-customer-website--digital-menu)
   - [2. Table Reservations & Inquiries](#2-table-reservations--inquiries)
   - [3. Point of Sale (POS) & 80mm Receipt Generator](#3-point-of-sale-pos--80mm-receipt-generator)
   - [4. Sales History & Buyer Transactions](#4-sales-history--buyer-transactions)
   - [5. Menu & Dynamic Category Management](#5-menu--dynamic-category-management)
   - [6. Business Events & Multi-Photo Albums](#6-business-events--multi-photo-albums)
   - [7. On-Demand Delivery Dispatch & Live Tracking](#7-on-demand-delivery-dispatch--live-tracking)
   - [8. Role-Based Staff Management & Security](#8-role-based-staff-management--security)
   - [9. Brand Identity & UI Design System](#9-brand-identity--ui-design-system)
5. [Database Migrations (Flyway)](#database-migrations-flyway)
6. [Environment Variables Reference](#environment-variables-reference)
7. [System Update Changelog](#system-update-changelog)

---

## System Architecture & Tech Stack

The system is engineered as a high-performance **Feature-Modular Monolith** optimized for low memory overhead, instant UI reactivity, and clean maintainability:

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Backend Framework** | Spring Boot 4.1.2 MVC | Spring Framework 7 / Jakarta EE 11 |
| **Language Runtime** | Java 25 (LTS Bytecode) | Managed via Maven Wrapper (`./mvnw` / `mvnw.cmd`) |
| **Server-Side UI** | Thymeleaf + HTMX 2.0 | Partial page rendering and snappy reactive updates without heavy JS SPA frameworks |
| **Styling & Design** | Tailwind CSS v4 | Precompiled via `@tailwindcss/cli`, custom warm bistro color scheme |
| **Database (Dev)** | Embedded H2 (PostgreSQL Mode) | Stored locally in `./data/ilonggobistro.mv.db` (zero external DB installation required) |
| **Database (Prod)** | PostgreSQL 16+ | Configurable via `DATABASE_URL` |
| **Schema Evolution** | Flyway Migrations | Versioned SQL scripts in `src/main/resources/db/migration/` |
| **Security** | Spring Security 7 | Role-Based Access Control (RBAC), BCrypt password hashing, CSRF protection |
| **Mapping & Location** | Leaflet + OpenStreetMap | Interactive customer pin drop for live delivery fee calculations |

---

## Project Structure

```text
Ilonggo Bistro/
├── .mvn/                                # Maven wrapper configuration
├── data/                                # Local embedded H2 database storage
├── src/
│   ├── main/
│   │   ├── java/com/ilonggobistro/
│   │   │   ├── admin/                   # Staff management, auth, security roles
│   │   │   ├── config/                  # Spring Security, Web, Data Seeder
│   │   │   ├── delivery/                # Courier dispatch, pandago/Grab/Simulator, map quotes
│   │   │   ├── event/                   # Business event albums & photo moderation
│   │   │   ├── gallery/                 # Showcase gallery photos
│   │   │   ├── home/                    # Landing page controller
│   │   │   ├── inquiry/                 # Customer contact messages
│   │   │   ├── menu/                    # Menu items and dynamic categories
│   │   │   ├── pos/                     # Point of Sale, receipts, order history
│   │   │   ├── reservation/             # Dine-in table reservations
│   │   │   └── shared/                  # Common services (Store profile, hours, uploads)
│   │   └── resources/
│   │       ├── db/migration/            # Flyway SQL migration scripts (V1 – V6)
│   │       ├── static/                  # Compiled CSS, JS libraries, uploaded media, logo
│   │       │   ├── css/                 # output.css (built via Tailwind CLI)
│   │       │   ├── images/              # logo.jpg, placeholder banners
│   │       │   └── uploads/             # Event photos, menu item images
│   │       ├── templates/               # Thymeleaf HTML views
│   │       │   ├── admin/               # Back-office admin views (POS, Menu, Events, Staff)
│   │       │   ├── fragments/           # Reusable components (navbar, footer, receipts)
│   │       │   └── ...                  # Customer pages (home, menu, order, events, tracker)
│   │       └── application.properties   # Core configuration & simulator defaults
│   └── test/                            # JUnit 5 & MockMvc automated test suite
├── Dockerfile                           # Multi-stage production container build
├── DOCUMENTATION.md                     # This file (Living system documentation & changelog)
├── package.json                         # Tailwind CSS CLI build scripts
├── pom.xml                              # Maven project dependencies
└── README.md                            # Quickstart guide
```

---

## Quick Start & Running the Project

### Prerequisites
- **Java 21 or Java 25 JDK** installed and available on `PATH`.
- **Node.js (v18+) & npm** (only needed when modifying CSS styles in `input.css`).

### 1. Build CSS (Tailwind CLI)
If you made changes to HTML templates or CSS utility classes:
```powershell
npm.cmd run css:build
```

### 2. Start the Application
Run using the Maven Wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```
The application will start on **`http://localhost:8080`**.

### 3. Running Automated Tests
```powershell
.\mvnw.cmd test -B
```

### 4. Admin Access
- URL: `http://localhost:8080/admin`
- Default Dev Username: `admin`
- Default Dev Password: `bistro2026!`

---

## Core Modules & Features

### 1. Customer Website & Digital Menu
- **Homepage (`/`)**: Hero section with authentic Ilonggo storytelling, daily operating schedule badge (7:00 AM – 9:00 PM), featured specials, customer testimonials, and direct action buttons.
- **QR Digital Menu (`/menu`)**: Live category filter (All, Mains, Breakfast Silog, Merienda, Drinks, Desserts), spicy/special tags, price display in Philippine Pesos (`₱`), and instant search.
- **Location & Hours**: Embedded Google Maps link with Plus Code (`VPW2+QF`) and Barotac Nuevo address.

### 2. Table Reservations & Inquiries
- **Reservations (`/reservations`)**: Dine-in reservation form capturing guest name, contact number, email, date, time slot, party size, and special requests.
- **Admin Management (`/admin/reservations`)**: Staff can view, confirm, seat, or cancel guest bookings with automated status transitions.
- **Contact Inquiries (`/contact`, `/admin/inquiries`)**: Customer feedback and catering inquiries form with admin inbox.

### 3. Point of Sale (POS) & 80mm Receipt Generator
- **Terminal (`/admin/pos`)**: High-speed touch and click interface for cashiers. Search dishes, tap to add to cart, adjust quantities, and switch between **Dine-In**, **Takeout**, and **Delivery**.
- **Senior Citizen / PWD 20% Discount**: Checkbox discount calculator complying with Philippine statutory discount regulations.
- **Cash Tender & Change Calculator**: Displays required cash change in real time.
- **80mm Thermal Receipt Generator**:
  - Auto-formatted receipt with official Bistro logo, store header, transaction number (`#ORD-...`), breakdown of items, discounts, VAT summary, and barcode aesthetic.
  - One-click browser print dialog (`window.print()`) styled specifically for thermal POS receipt printers.
  - Receipt re-print capability accessible from sales history.

### 4. Sales History & Buyer Transactions
- **Sales Ledger (`/admin/sales`)**: Real-time log of all completed transactions.
- Filter by date, order type, payment method (Cash, GCash, Maya, Card), and order status (`COMPLETED`, `VOIDED`).
- Detailed breakdown displaying cashier name, customer name, Senior/PWD ID details, and itemized lines.

### 5. Menu & Dynamic Category Management
- **Category Manager (`/admin/categories`)**: Cashiers and managers can create new categories on the fly (e.g., "Seasonal Specials", "Combo Meals") with custom display order, slug, and descriptions.
- **Menu Item Manager (`/admin/menu`)**: Add/edit dishes, upload photos, set prices, mark items as available or out of stock (`soldOut`), and assign categories.

### 6. Business Events & Multi-Photo Albums
- **Event Albums (`/events`, `/admin/events`)**: Dedicated showcase for restaurant events (e.g., Dining Nights, Awards Dinners, Private Catering).
- **Multi-Photo Batch Upload**: Drag-and-drop or select multiple photos simultaneously. Files are processed and stored under `static/uploads/events/`.
- **Guest Community Upload & Moderation**: Customers can upload photos from events; photos remain in `PENDING` state until approved by an Admin in `/admin/events`.

### 7. On-Demand Delivery Dispatch & Live Tracking
- **Interactive Checkout (`/order`)**:
  - Leaflet / OpenStreetMap pin picker with live reverse-geocoding for Barotac Nuevo and surrounding Iloilo municipalities.
  - Real-time delivery quote (`POST /api/delivery/quote`) calculating fee based on distance and estimated travel time.
- **Rider Dispatch Service**:
  - Multi-provider architecture supporting **pandago**, **GrabExpress**, and **In-House Local Rider**.
  - **Zero-Key Local Simulator**: Pre-configured out of the box so checkout and dispatch can be tested without active API keys.
  - Cashier dispatch console (`/admin/deliveries`) with "Dispatch Rider" button and manual status overrides.
- **Live Customer Tracker (`/orders/{orderRef}`)**:
  - Live progress stepper (`ALLOCATING` → `RIDER_ASSIGNED` → `PICKED_UP` → `DELIVERED`).
  - Displays assigned rider name, phone number, vehicle plate number, and live tracking map link.
- **Courier Webhook Receiver (`POST /api/webhooks/delivery`)**:
  - Receives asynchronous status callbacks from courier APIs and updates delivery states automatically.

### 8. Role-Based Staff Management & Security
- **Granular Permissions (Spring Security)**:
  - **`ADMIN`**: Full platform control (staff accounts, store settings, voiding sales, system logs).
  - **`CASHIER`**: POS terminal, receipt printing, rider dispatch, view-only sales history.
  - **`BOOKING_HOST`**: Manage table reservations and guest inquiries.
  - **`KITCHEN_STAFF`**: Live kitchen display orders, menu item availability toggling.
- **Staff Account Management (`/admin/staff`)**: Register employees, assign roles, reset credentials, and launch quick WhatsApp chats via `wa.me/<number>`.
- **Dual-Channel OTP Verification (Gmail & WhatsApp)**:
  - Expiring 6-digit cryptographic tokens for password changes and account recovery.
  - Built-in zero-key dev simulator banner showing OTP on screen during testing.
- **Sign in with Google (OAuth2 Client)**:
  - Allows staff to authenticate using their verified Gmail address if pre-registered by the Admin.

### 9. Brand Identity & UI Design System
- **Official Logo**: High-resolution brand emblem featuring chef hat, fork, spoon, and tropical greenery (`/images/logo.jpg`).
- **Color Palette**:
  - Primary Bistro Green: `#1d3b2a`
  - Terracotta Rust: `#c85a2b`
  - Warm Cream Background: `#faf8f4`
- **Responsive Layout**: Mobile-first responsive header with streamlined navigation, sticky call-to-actions, and slide-over mobile drawer.

---

## Database Migrations (Flyway)

All database changes are versioned and automatically applied on application launch:

| Migration File | Description |
| :--- | :--- |
| `V1__create_core_tables.sql` | Core schema: `menu_categories`, `menu_items`, `reservations`, `gallery_photos`, `contact_inquiries`, `store_settings`, `admin_users`. |
| `V2__seed_initial_data.sql` | Seeded Ilonggo specialties (Batchoy, KBL, Chicken Inasal, Kansi), initial categories, store hours, and default admin. |
| `V3__add_pos_orders.sql` | Point of Sale schema: `pos_orders` and `pos_order_items` supporting discounts, payment methods, and receipts. |
| `V4__add_business_events.sql` | Event showcase schema: `business_events` and `event_photos` with approval status for moderation. |
| `V5__add_delivery_dispatch.sql` | Courier logistics schema: `deliveries` table linked to orders with GPS coordinates and tracking metadata. |
| `V6__staff_roles_and_verification.sql` | Staff RBAC expansion: roles, mobile/email verification, and `auth_otp_tokens` for password recovery. |

---

## Environment Variables Reference

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP port for the web server |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile (`dev` or `prod`) |
| `DATABASE_URL` | H2 embedded file | JDBC connection string |
| `APP_ADMIN_USERNAME` | `admin` | Initial superadmin username |
| `APP_ADMIN_PASSWORD` | `bistro2026!` | Initial superadmin password |
| `DELIVERY_PROVIDER` | `SIMULATOR` | Courier provider: `SIMULATOR`, `PANDAGO`, `GRABEXPRESS`, `IN_HOUSE` |
| `API_CLIENT_ID` | *(empty)* | Courier API client key |
| `API_CLIENT_SECRET` | *(empty)* | Courier API secret |
| `MAIL_PROVIDER` | `SIMULATOR` | Email provider: `SIMULATOR` or `SMTP` |
| `WHATSAPP_PROVIDER` | `SIMULATOR` | WhatsApp provider: `SIMULATOR` or `META_CLOUD` |
| `GOOGLE_CLIENT_ID` | *(empty)* | Google OAuth2 Client ID |
| `GOOGLE_CLIENT_SECRET` | *(empty)* | Google OAuth2 Client Secret |

---

## System Update Changelog

> **Notice for Developers & AI Agents:**  
> When performing updates to the system, log your release below following this template:
> ```markdown
> ### [Version] — YYYY-MM-DD
> - **Module:** Name of module
> - **Type:** Feature | Bugfix | Refactor | Styling
> - **Summary:** Brief description of what was added or modified.
> - **Key Files Modified:** List of files touched.
> ```

---

### [1.6.0] — 2026-11-19
- **Module:** Security, Staff Management & Authentication
- **Type:** Feature & Security
- **Summary:** Added granular Role-Based Access Control (`ADMIN`, `CASHIER`, `BOOKING_HOST`, `KITCHEN_STAFF`), staff account manager at `/admin/staff`, dual-channel OTP verification (Gmail + WhatsApp) for password resets, and "Sign in with Google" OAuth2 integration with dev simulator fallbacks.
- **Key Files Modified:**
  - `src/main/resources/db/migration/V6__staff_roles_and_verification.sql`
  - `src/main/java/com/ilonggobistro/config/SecurityConfig.java`
  - `src/main/java/com/ilonggobistro/admin/StaffRole.java`
  - `src/main/java/com/ilonggobistro/admin/AdminUser.java`
  - `src/main/resources/templates/admin/admin-layout.html`

---

### [1.5.0] — 2026-11-19
- **Module:** UI & Brand Identity
- **Type:** Styling & Refactor
- **Summary:** Cleaned up and streamlined the customer navigation bar into a unified single-row layout with a slim top utility strip. Replaced temporary logo with the official high-resolution Ilonggo Bistro artwork across navbar, footer, POS receipts, and login screens.
- **Key Files Modified:**
  - `src/main/resources/static/images/logo.jpg`
  - `src/main/resources/templates/fragments/navbar.html`
  - `src/main/resources/templates/fragments/footer.html`
  - `src/main/resources/templates/admin/admin-login.html`
  - `src/main/resources/templates/admin/pos-receipt.html`

---

### [1.4.0] — 2026-11-19
- **Module:** Delivery Dispatch & Map Tracking
- **Type:** Feature
- **Summary:** Built the on-demand courier delivery dispatch system. Added Leaflet map pin address selector, live delivery fee calculation endpoint (`/api/delivery/quote`), pandago/GrabExpress/In-House courier dispatch service, live customer tracking stepper (`/orders/{ref}`), and cashier dispatch management (`/admin/deliveries`).
- **Key Files Modified:**
  - `src/main/resources/db/migration/V5__add_delivery_dispatch.sql`
  - `src/main/java/com/ilonggobistro/delivery/DeliveryService.java`
  - `src/main/java/com/ilonggobistro/delivery/CustomerOrderController.java`
  - `src/main/java/com/ilonggobistro/delivery/AdminDeliveryController.java`
  - `src/main/resources/templates/order-checkout.html`
  - `src/main/resources/templates/order-tracking.html`

---

### [1.3.0] — 2026-11-19
- **Module:** Business Events & Community Photos
- **Type:** Feature
- **Summary:** Added the Business Events album module with multi-photo batch upload, event creation, guest photo submissions, and an admin moderation workflow.
- **Key Files Modified:**
  - `src/main/resources/db/migration/V4__add_business_events.sql`
  - `src/main/java/com/ilonggobistro/event/BusinessEventService.java`
  - `src/main/java/com/ilonggobistro/event/AdminEventController.java`
  - `src/main/resources/templates/events.html`
  - `src/main/resources/templates/admin/events-list.html`

---

### [1.2.0] — 2026-11-19
- **Module:** Point of Sale (POS) & Category Management
- **Type:** Feature
- **Summary:** Developed the touch POS cashier terminal with Senior/PWD 20% discount calculations, change computation, printable 80mm thermal receipts, customer purchase history (`/admin/sales`), and dynamic category creation/ordering (`/admin/categories`).
- **Key Files Modified:**
  - `src/main/resources/db/migration/V3__add_pos_orders.sql`
  - `src/main/java/com/ilonggobistro/pos/AdminPosController.java`
  - `src/main/java/com/ilonggobistro/pos/PosService.java`
  - `src/main/java/com/ilonggobistro/admin/AdminCategoryController.java`
  - `src/main/resources/templates/admin/pos.html`
  - `src/main/resources/templates/admin/pos-receipt.html`
  - `src/main/resources/templates/admin/sales-list.html`

---

### [1.1.0] — 2026-11-19
- **Module:** Initial System Core & Architecture
- **Type:** Initial Release
- **Summary:** Initial bootstrap of the Ilonggo Bistro web platform. Built Spring Boot 4.1.2 MVC application with H2 database, Flyway migrations, digital menu, table reservations, contact inquiries, and basic admin authentication.
- **Key Files Modified:**
  - `pom.xml`
  - `src/main/resources/db/migration/V1__create_core_tables.sql`
  - `src/main/resources/db/migration/V2__seed_initial_data.sql`
  - `src/main/java/com/ilonggobistro/IlonggoBistroApplication.java`
  - `src/main/resources/templates/home.html`
  - `src/main/resources/templates/menu.html`
  - `src/main/resources/templates/reservations.html`
