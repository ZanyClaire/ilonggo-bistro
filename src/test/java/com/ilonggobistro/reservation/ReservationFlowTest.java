package com.ilonggobistro.reservation;

import com.ilonggobistro.shared.BusinessHoursService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessHoursService businessHoursService;

    @Test
    void guestCanBookLookupAndCancelReservation() throws Exception {
        LocalDate tomorrow = businessHoursService.today().plusDays(2);

        MvcResult result = mockMvc.perform(post("/reservations")
                        .with(csrf())
                        .param("guestName", "Elena Reyes")
                        .param("phone", "0917 555 8921")
                        .param("email", "elena@example.com")
                        .param("partySize", "4")
                        .param("reservationDate", tomorrow.toString())
                        .param("reservationTime", "12:00")
                        .param("seatingPreference", "Window Table")
                        .param("specialRequests", "Birthday lunch"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/reservations/confirmed/IB-*"))
                .andReturn();

        String redirectedUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        String refCode = redirectedUrl.substring(redirectedUrl.lastIndexOf('/') + 1);

        // Confirmation screen displays code and guest name
        mockMvc.perform(get(redirectedUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(refCode)))
                .andExpect(content().string(containsString("Elena Reyes")));

        // Cancel with verified phone last 4 digits
        mockMvc.perform(post("/reservations/{ref}/cancel", refCode)
                        .with(csrf())
                        .param("phoneLastDigits", "8921"))
                .andExpect(status().is3xxRedirection());

        // Lookup reflects Cancelled status
        mockMvc.perform(get("/reservations").param("ref", refCode))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cancelled")));
    }

    @Test
    void rejectsReservationOutsideOperatingHours() throws Exception {
        LocalDate tomorrow = businessHoursService.today().plusDays(3);

        mockMvc.perform(post("/reservations")
                        .with(csrf())
                        .param("guestName", "Late Diner")
                        .param("phone", "0917 555 1111")
                        .param("partySize", "2")
                        .param("reservationDate", tomorrow.toString())
                        .param("reservationTime", "22:00")
                        .param("seatingPreference", "Main Dining"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("outside our reservation hours")));
    }
}
