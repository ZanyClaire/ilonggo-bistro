package com.ilonggobistro.config;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import com.ilonggobistro.delivery.Delivery;
import com.ilonggobistro.delivery.DeliveryProperties;
import com.ilonggobistro.delivery.DeliveryProvider;
import com.ilonggobistro.delivery.DeliveryRepository;
import com.ilonggobistro.menu.MenuItem;
import com.ilonggobistro.menu.MenuItemRepository;
import com.ilonggobistro.pos.OrderType;
import com.ilonggobistro.pos.PaymentMethod;
import com.ilonggobistro.pos.PosOrder;
import com.ilonggobistro.pos.PosOrderItem;
import com.ilonggobistro.pos.PosOrderRepository;
import com.ilonggobistro.reservation.Reservation;
import com.ilonggobistro.reservation.ReservationRepository;
import com.ilonggobistro.reservation.ReservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AdminUserRepository adminUserRepository;
    private final ReservationRepository reservationRepository;
    private final PosOrderRepository posOrderRepository;
    private final MenuItemRepository menuItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryProperties deliveryProperties;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Environment environment;

    @Value("${app.admin.default-username:admin}")
    private String defaultUsername;

    @Value("${app.admin.default-password:bistro2026!}")
    private String defaultPassword;

    @Value("${app.admin.default-email:ilonggobistro.admin@gmail.com}")
    private String defaultEmail;

    @Value("${app.admin.default-phone:+63 917 800 2025}")
    private String defaultPhone;

    public DataSeeder(AdminUserRepository adminUserRepository,
                      ReservationRepository reservationRepository,
                      PosOrderRepository posOrderRepository,
                      MenuItemRepository menuItemRepository,
                      DeliveryRepository deliveryRepository,
                      DeliveryProperties deliveryProperties,
                      PasswordEncoder passwordEncoder,
                      Clock clock,
                      Environment environment) {
        this.adminUserRepository = adminUserRepository;
        this.reservationRepository = reservationRepository;
        this.posOrderRepository = posOrderRepository;
        this.menuItemRepository = menuItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryProperties = deliveryProperties;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!adminUserRepository.existsByUsernameIgnoreCase(defaultUsername)) {
            AdminUser owner = new AdminUser(
                    defaultUsername,
                    passwordEncoder.encode(defaultPassword),
                    "Ilonggo Bistro Owner",
                    defaultEmail,
                    defaultPhone,
                    "ADMIN",
                    false
            );
            owner.setEmailVerified(true);
            owner.setPhoneVerified(true);
            adminUserRepository.save(owner);
            log.info("Seeded default admin account '{}' ({}). Change password via APP_ADMIN_PASSWORD in production.",
                    defaultUsername, defaultEmail);
        }

        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev")
                || environment.getActiveProfiles().length == 0;
        if (!isDev) {
            return;
        }

        // Seed sample role-based dev staff accounts so RBAC can be tested immediately on localhost
        seedDevStaffIfMissing("cashier", "cashier2026!", "Joy Villanueva", "ilonggobistro.cashier@gmail.com", "+63 917 800 2026", "CASHIER");
        seedDevStaffIfMissing("host", "host2026!", "Paolo Mendoza", "ilonggobistro.host@gmail.com", "+63 917 800 2027", "BOOKING_HOST");
        seedDevStaffIfMissing("kitchen", "kitchen2026!", "Chef Rico Alcantara", "ilonggobistro.kitchen@gmail.com", "+63 917 800 2028", "KITCHEN_STAFF");

        if (reservationRepository.count() == 0) {
            LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
            Reservation r1 = new Reservation(
                    "IB-2026-BN01", "Marites Villanueva", "0917 482 1903", "marites@example.com",
                    4, tomorrow, LocalTime.of(8, 30), "Window Table",
                    "Morning coffee & tapsilog after market run."
            );
            r1.setStatus(ReservationStatus.CONFIRMED);

            Reservation r2 = new Reservation(
                    "IB-2026-BN02", "Carlo Jalandoni", "0928 773 4109", "carlo.j@example.com",
                    6, tomorrow, LocalTime.of(12, 0), "Family / Barkada Table",
                    "Pre-ordering 2 pots of KBL if available."
            );
            r2.setStatus(ReservationStatus.PENDING);

            Reservation r3 = new Reservation(
                    "IB-2026-BN03", "Bianca Araneta", "0919 601 8842", null,
                    2, tomorrow.plusDays(1), LocalTime.of(18, 30), "Main Dining",
                    "Anniversary dinner — quiet table near window."
            );
            r3.setStatus(ReservationStatus.PENDING);

            reservationRepository.save(r1);
            reservationRepository.save(r2);
            reservationRepository.save(r3);
        }

        if (posOrderRepository.count() == 0) {
            List<MenuItem> items = menuItemRepository.findAllByOrderByDisplayOrderAscNameAsc();
            if (items.size() >= 3) {
                MenuItem batchoy = items.get(0);
                MenuItem inasal = items.get(1);
                MenuItem kbl = items.get(2);
                LocalDateTime now = LocalDateTime.now(clock);

                BigDecimal o1Total = batchoy.getPricePhp().multiply(BigDecimal.valueOf(2)).add(inasal.getPricePhp());
                PosOrder o1 = new PosOrder(
                        "OR-2026-00101", "Ramon Lopez", "0917 210 4412",
                        OrderType.DINE_IN, "Table 3", PaymentMethod.CASH,
                        o1Total, null, BigDecimal.ZERO, o1Total,
                        new BigDecimal("1000.00"), new BigDecimal("1000.00").subtract(o1Total),
                        defaultUsername, "Extra toasted garlic on batchoy", now.minusHours(2)
                );
                o1.addItem(new PosOrderItem(batchoy.getId(), batchoy.getName(), batchoy.getPricePhp(), 2,
                        batchoy.getPricePhp().multiply(BigDecimal.valueOf(2))));
                o1.addItem(new PosOrderItem(inasal.getId(), inasal.getName(), inasal.getPricePhp(), 1,
                        inasal.getPricePhp()));

                BigDecimal o2Total = kbl.getPricePhp().add(inasal.getPricePhp());
                PosOrder o2 = new PosOrder(
                        "OR-2026-00102", "Grace Montelibano", "0998 411 9034",
                        OrderType.TAKEOUT, "Takeout", PaymentMethod.GCASH,
                        o2Total, null, BigDecimal.ZERO, o2Total,
                        o2Total, BigDecimal.ZERO,
                        defaultUsername, "Separate broth container", now.minusMinutes(35)
                );
                o2.addItem(new PosOrderItem(kbl.getId(), kbl.getName(), kbl.getPricePhp(), 1, kbl.getPricePhp()));
                o2.addItem(new PosOrderItem(inasal.getId(), inasal.getName(), inasal.getPricePhp(), 1, inasal.getPricePhp()));

                posOrderRepository.save(o1);
                posOrderRepository.save(o2);
            }
        }

        if (deliveryRepository.count() == 0) {
            List<MenuItem> items = menuItemRepository.findAllByOrderByDisplayOrderAscNameAsc();
            if (items.size() >= 2) {
                MenuItem batchoy = items.get(0);
                MenuItem inasal = items.get(1);
                BigDecimal sub = batchoy.getPricePhp().add(inasal.getPricePhp());
                BigDecimal fee = new BigDecimal("57.00");
                BigDecimal total = sub.add(fee);

                PosOrder delOrder = new PosOrder(
                        "OR-2026-DEL01", "Miguel Soriano", "0917 654 2210",
                        OrderType.DELIVERY, "Delivery", PaymentMethod.CASH,
                        sub, null, BigDecimal.ZERO, total,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        "online-web", "Ring gate bell upon arrival", LocalDateTime.now(clock).minusMinutes(12)
                );
                delOrder.setDeliveryFeePhp(fee);
                delOrder.setPaymentStatus("CASH_ON_DELIVERY");
                delOrder.addItem(new PosOrderItem(batchoy.getId(), batchoy.getName(), batchoy.getPricePhp(), 1, batchoy.getPricePhp()));
                delOrder.addItem(new PosOrderItem(inasal.getId(), inasal.getName(), inasal.getPricePhp(), 1, inasal.getPricePhp()));
                PosOrder savedDelOrder = posOrderRepository.save(delOrder);

                Delivery sampleDelivery = new Delivery(
                        savedDelOrder,
                        DeliveryProvider.SIMULATOR,
                        deliveryProperties.getStoreAddress(),
                        deliveryProperties.getStoreLat(),
                        deliveryProperties.getStoreLng(),
                        "Poblacion Ilaya, Barotac Nuevo, 5007 Iloilo",
                        "Blue gate beside Barotac Nuevo Football Field",
                        10.8924,
                        122.7078,
                        new BigDecimal("2.85"),
                        fee,
                        28
                );
                deliveryRepository.save(sampleDelivery);
            }
        }
    }

    private void seedDevStaffIfMissing(String username, String rawPassword, String fullName,
                                       String email, String phone, String role) {
        if (!adminUserRepository.existsByUsernameIgnoreCase(username)
                && !adminUserRepository.existsByEmailIgnoreCase(email)) {
            AdminUser staff = new AdminUser(
                    username,
                    passwordEncoder.encode(rawPassword),
                    fullName,
                    email,
                    phone,
                    role,
                    false
            );
            staff.setEmailVerified(true);
            staff.setPhoneVerified(true);
            adminUserRepository.save(staff);
        }
    }
}
