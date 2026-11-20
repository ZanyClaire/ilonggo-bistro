package com.ilonggobistro.admin;

import com.ilonggobistro.delivery.DeliveryService;
import com.ilonggobistro.event.BusinessEventService;
import com.ilonggobistro.inquiry.InquiryService;
import com.ilonggobistro.menu.MenuService;
import com.ilonggobistro.pos.PosService;
import com.ilonggobistro.reservation.ReservationService;
import com.ilonggobistro.shared.BusinessHoursService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final ReservationService reservationService;
    private final MenuService menuService;
    private final InquiryService inquiryService;
    private final BusinessHoursService businessHoursService;
    private final PosService posService;
    private final BusinessEventService businessEventService;
    private final DeliveryService deliveryService;

    public AdminDashboardController(ReservationService reservationService,
                                    MenuService menuService,
                                    InquiryService inquiryService,
                                    BusinessHoursService businessHoursService,
                                    PosService posService,
                                    BusinessEventService businessEventService,
                                    DeliveryService deliveryService) {
        this.reservationService = reservationService;
        this.menuService = menuService;
        this.inquiryService = inquiryService;
        this.businessHoursService = businessHoursService;
        this.posService = posService;
        this.businessEventService = businessEventService;
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("todayBookingsCount", reservationService.countTodayActive());
        model.addAttribute("pendingBookingsCount", reservationService.countPending());
        model.addAttribute("availableDishesCount", menuService.countAvailable());
        model.addAttribute("soldOutDishesCount", menuService.countSoldOut());
        model.addAttribute("unreadInquiriesCount", inquiryService.countUnread());
        model.addAttribute("pendingEventPhotosCount", businessEventService.countPendingGuestPhotos());
        model.addAttribute("activeDeliveriesCount", deliveryService.countActiveDeliveries());
        model.addAttribute("salesSummary", posService.getSalesSummary());
        model.addAttribute("recentOrders", posService.listRecent());
        model.addAttribute("todayReservations", reservationService.listForDate(businessHoursService.today()));
        model.addAttribute("upcomingReservations", reservationService.listUpcoming().stream().limit(8).toList());
        return "admin/dashboard";
    }
}
