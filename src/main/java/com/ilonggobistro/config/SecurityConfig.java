package com.ilonggobistro.config;

import com.ilonggobistro.auth.StaffLoginSuccessHandler;
import com.ilonggobistro.auth.StaffOidcUserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    private final StaffOidcUserService staffOidcUserService;
    private final StaffLoginSuccessHandler loginSuccessHandler;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public SecurityConfig(StaffOidcUserService staffOidcUserService,
                          StaffLoginSuccessHandler loginSuccessHandler,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.staffOidcUserService = staffOidcUserService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> {
                    // 1. Public website, APIs, login & forgot-password flows
                    auth.requestMatchers(
                            "/", "/menu/**", "/reservations/**", "/gallery/**", "/events/**",
                            "/order/**", "/orders/**", "/api/delivery/quote", "/api/webhooks/delivery",
                            "/about", "/contact", "/sitemap.xml",
                            "/css/**", "/js/**", "/images/**", "/uploads/**",
                            "/manifest.webmanifest", "/sw-pos.js",
                            "/actuator/health", "/error", "/error/**",
                            "/admin/login", "/admin/forgot-password/**", "/admin/dev-google-login"
                    ).permitAll();

                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }

                    // 2. All authenticated staff roles
                    auth.requestMatchers("/admin", "/admin/", "/admin/account/**", "/admin/access-denied")
                            .hasAnyRole("ADMIN", "CASHIER", "BOOKING_HOST", "KITCHEN_STAFF");

                    // 3. Strictly ADMIN-only routes
                    auth.requestMatchers("/admin/staff/**", "/admin/settings/**", "/admin/events/**", "/admin/gallery/**")
                            .hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/admin/sales/*/void").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/admin/sales/**").hasRole("ADMIN");

                    // 4. POS & Read-Only Sales History (ADMIN + CASHIER)
                    auth.requestMatchers("/admin/pos/**").hasAnyRole("ADMIN", "CASHIER");
                    auth.requestMatchers(HttpMethod.GET, "/admin/sales", "/admin/sales/**").hasAnyRole("ADMIN", "CASHIER");

                    // 5. Deliveries (View-only for KITCHEN_STAFF; full dispatch for ADMIN + CASHIER)
                    auth.requestMatchers(HttpMethod.GET, "/admin/deliveries", "/admin/deliveries/**")
                            .hasAnyRole("ADMIN", "CASHIER", "KITCHEN_STAFF");
                    auth.requestMatchers(HttpMethod.POST, "/admin/deliveries/**")
                            .hasAnyRole("ADMIN", "CASHIER");

                    // 6. Bookings & Inquiries (ADMIN + BOOKING_HOST)
                    auth.requestMatchers("/admin/reservations/**", "/admin/inquiries/**")
                            .hasAnyRole("ADMIN", "BOOKING_HOST");

                    // 7. Kitchen Display Queue (KDS) — ADMIN, KITCHEN_STAFF, CASHIER (BOOKING_HOST blocked -> 403)
                    auth.requestMatchers("/admin/kitchen", "/admin/kitchen/**")
                            .hasAnyRole("ADMIN", "KITCHEN_STAFF", "CASHIER");

                    // 8. Kitchen Menu & Categories (ADMIN + KITCHEN_STAFF)
                    auth.requestMatchers("/admin/menu/**", "/admin/categories/**")
                            .hasAnyRole("ADMIN", "KITCHEN_STAFF");

                    // 9. Catch-all for any future /admin/** routes
                    auth.requestMatchers("/admin/**").hasRole("ADMIN");
                    auth.anyRequest().permitAll();
                })
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .successHandler(loginSuccessHandler)
                        .failureUrl("/admin/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            request.setAttribute("deniedPath", request.getRequestURI());
                            request.getRequestDispatcher("/admin/access-denied").forward(request, response);
                        })
                )
                .headers(headers -> {
                    if (h2ConsoleEnabled) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    }
                });

        // Enable real Google OAuth2 Login ONLY when GOOGLE_CLIENT_ID & GOOGLE_CLIENT_SECRET are configured
        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/admin/login")
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(staffOidcUserService))
                    .successHandler(loginSuccessHandler)
                    .failureHandler((request, response, exception) -> {
                        String msg = exception.getMessage() != null ? exception.getMessage() : "Google sign-in failed.";
                        String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);
                        response.sendRedirect("/admin/login?googleError=" + encoded);
                    })
            );
        }

        if (h2ConsoleEnabled) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/webhooks/delivery", "/api/delivery/quote"));
        } else {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/webhooks/delivery", "/api/delivery/quote"));
        }

        return http.build();
    }
}
