# Ilonggo Bistro — Web Platform

Modern **Java / Spring Boot MVC** web platform and staff operations portal for **Ilonggo Bistro** (*Burgos-Osmeña St, Barotac Nuevo, 5007 Iloilo · Plus Code `VPW2+QF` · Daily 7:00 AM – 9:00 PM*).

> 📖 **Full System Documentation & Update Changelog:**  
> For comprehensive module documentation, architecture specifications, API references, and release history, refer to [DOCUMENTATION.md](file:///c:/Users/alexa/Documents/Antigravity/Iloggo%20Bistro/DOCUMENTATION.md).

## Architecture & Tech Stack
- **Architecture:** Feature-Modular Monolith (**Spring Boot 4.1 Web MVC + Thymeleaf + HTMX + Tailwind CSS v4**)
- **Java:** Java 25 LTS target via Maven Wrapper (`./mvnw` / `mvnw.cmd`)
- **Database:** Embedded **H2** (PostgreSQL compatibility mode) in `dev` profile · **PostgreSQL** in `prod` profile
- **Migrations:** **Flyway** (`src/main/resources/db/migration/`)
- **Security:** **Spring Security** with BCrypt staff credentials and CSRF protection (including HTMX headers)

## Local Quick Start

1. **Build CSS** (only needed after editing templates or `input.css`):
   ```powershell
   npm.cmd run css:build
   ```
2. **Run the application** (uses embedded H2 — zero external DB setup required):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. **Open in browser:**
   - Public Website: `http://localhost:8080`
   - Digital Menu (QR-ready): `http://localhost:8080/menu`
   - Table Reservations: `http://localhost:8080/reservations`
   - Staff Admin Portal: `http://localhost:8080/admin`
     - Default dev login: **`admin`** / **`bistro2026!`** (override via `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`)

## Running Automated Tests
```powershell
.\mvnw.cmd test
```

## Production Deployment (Railway / Render / Docker)
Build and run the included multi-stage `Dockerfile` with environment variables:
- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL=jdbc:postgresql://<host>:5432/<db>`
- `DATABASE_USERNAME=<user>`
- `DATABASE_PASSWORD=<password>`
- `APP_ADMIN_PASSWORD=<strong-password>`

### On-Demand Delivery Dispatch Environment Variables
By default, delivery runs in built-in **Sandbox Simulator + In-House Local Rider** mode (no API keys required). When you obtain courier credentials, set:
- `DELIVERY_PROVIDER` — `SIMULATOR` (default), `PANDAGO`, `GRABEXPRESS`, or `IN_HOUSE`
- `API_CLIENT_ID` — Courier API client ID
- `API_CLIENT_SECRET` — Courier API client secret
- `DELIVERY_API_BASE_URL` — Optional override for sandbox vs. production endpoint
- `DELIVERY_WEBHOOK_SECRET` — Optional shared secret checked on `X-Delivery-Webhook-Secret` header at `POST /api/webhooks/delivery`
- `STORE_LAT` / `STORE_LNG` / `STORE_ADDRESS` — Restaurant pickup coordinates and address (defaults to Burgos-Osmeña St, Barotac Nuevo, Iloilo)
- `DELIVERY_AUTO_DISPATCH` — `false` (cashier clicks Dispatch Rider) or `true` (auto-dispatch on order placement)

### Staff Roles, Gmail/WhatsApp OTP & Google Sign-In Environment Variables
Runs in zero-key **Simulator mode** locally by default (dev OTP preview banners shown in `dev` profile):
- **Gmail SMTP:** `MAIL_PROVIDER` (`SIMULATOR` | `SMTP`), `MAIL_HOST` (`smtp.gmail.com`), `MAIL_PORT` (`587`), `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- **WhatsApp Business Cloud API:** `WHATSAPP_PROVIDER` (`SIMULATOR` | `META_CLOUD`), `WHATSAPP_API_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`
- **Sign in with Google (OAuth2):** `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (only enabled Gmail addresses registered in `/admin/staff` are allowed to sign in)
- **Default Dev Staff Accounts:**
  - `admin` / `bistro2026!` (`ADMIN` · `ilonggobistro.admin@gmail.com`)
  - `cashier` / `cashier2026!` (`CASHIER` · `ilonggobistro.cashier@gmail.com`)
  - `host` / `host2026!` (`BOOKING_HOST` · `ilonggobistro.host@gmail.com`)
  - `kitchen` / `kitchen2026!` (`KITCHEN_STAFF` · `ilonggobistro.kitchen@gmail.com`)

## Versioning & Safe Rollback Workflow (GitHub Tags)
Every stable state of the system is saved as an annotated Git version tag (`v1.0.0`, `v1.0.1`, `v1.1.0`, ...) before making new modifications:
- **Save & push a new version snapshot** (runs all tests, updates `pom.xml`, commits, tags, and pushes to GitHub):
  ```powershell
  .\scripts\save-version.ps1 -Version v1.0.1 -Message "Short description of changes"
  ```
- **List all saved versions:** `git tag -l -n`
- **Safely restore a previous version on a new branch:** `git switch -c restore-v1.0.0 v1.0.0`
