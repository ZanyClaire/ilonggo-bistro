package com.ilonggobistro.auth;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class OtpService {

    public static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthOtpTokenRepository tokenRepository;
    private final AdminUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthNotificationProperties properties;
    private final SimulatorMailAdapter simulatorMail;
    private final SmtpMailAdapter smtpMail;
    private final SimulatorWhatsAppAdapter simulatorWhatsApp;
    private final MetaCloudWhatsAppAdapter metaCloudWhatsApp;
    private final Environment environment;
    private final Clock clock;

    public OtpService(AuthOtpTokenRepository tokenRepository,
                      AdminUserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AuthNotificationProperties properties,
                      SimulatorMailAdapter simulatorMail,
                      SmtpMailAdapter smtpMail,
                      SimulatorWhatsAppAdapter simulatorWhatsApp,
                      MetaCloudWhatsAppAdapter metaCloudWhatsApp,
                      Environment environment,
                      Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.simulatorMail = simulatorMail;
        this.smtpMail = smtpMail;
        this.simulatorWhatsApp = simulatorWhatsApp;
        this.metaCloudWhatsApp = metaCloudWhatsApp;
        this.environment = environment;
        this.clock = clock;
    }

    @Transactional
    public OtpDispatchResult issueOtp(AdminUser user, OtpChannel channel, OtpPurpose purpose) {
        // Invalidate any prior unused tokens for the same user & purpose
        List<AuthOtpToken> existing = tokenRepository.findByUserIdAndPurposeAndUsedFalse(user.getId(), purpose);
        existing.forEach(t -> t.setUsed(true));
        if (!existing.isEmpty()) {
            tokenRepository.saveAll(existing);
        }

        String rawCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(OTP_EXPIRY_MINUTES);

        AuthOtpToken token = new AuthOtpToken(
                user,
                passwordEncoder.encode(rawCode),
                channel,
                purpose,
                expiresAt
        );
        tokenRepository.save(token);

        boolean simulated;
        String providerLabel;
        String maskedTarget;

        if (channel == OtpChannel.GMAIL) {
            MailDeliveryAdapter adapter = properties.isMailSmtpConfigured() ? smtpMail : simulatorMail;
            adapter.sendOtpEmail(user.getEmail(), user.getFullName(), rawCode, purpose, OTP_EXPIRY_MINUTES);
            simulated = adapter.isSimulated();
            providerLabel = adapter.providerLabel();
            maskedTarget = maskEmail(user.getEmail());
        } else {
            WhatsAppDeliveryAdapter adapter = properties.isWhatsAppMetaConfigured() ? metaCloudWhatsApp : simulatorWhatsApp;
            String e164 = user.whatsappDigits();
            if (e164.isBlank()) {
                throw new IllegalArgumentException("This account has no valid WhatsApp phone number on file.");
            }
            adapter.sendOtpWhatsApp(e164, user.getFullName(), rawCode, purpose, OTP_EXPIRY_MINUTES);
            simulated = adapter.isSimulated();
            providerLabel = adapter.providerLabel();
            maskedTarget = maskPhone(user.getPhoneNumber());
        }

        String devPreviewCode = (simulated && isDevOrTestProfile()) ? rawCode : null;
        return new OtpDispatchResult(channel, purpose, maskedTarget, providerLabel, simulated, devPreviewCode, OTP_EXPIRY_MINUTES);
    }

    @Transactional
    public OtpChannel verifyAndConsume(AdminUser user, OtpPurpose purpose, String rawOtpCode) {
        if (rawOtpCode == null || !rawOtpCode.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("Please enter the 6-digit verification code.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        AuthOtpToken token = tokenRepository
                .findFirstByUserIdAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user.getId(), purpose, now)
                .orElseThrow(() -> new IllegalArgumentException("No active verification code found or the code has expired. Please request a new code."));

        if (!passwordEncoder.matches(rawOtpCode.trim(), token.getOtpHash())) {
            throw new IllegalArgumentException("Incorrect 6-digit verification code. Please check and try again.");
        }

        token.setUsed(true);
        tokenRepository.save(token);

        // Mark contact channel verified
        if (token.getChannel() == OtpChannel.GMAIL && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        } else if (token.getChannel() == OtpChannel.WHATSAPP && !user.isPhoneVerified()) {
            user.setPhoneVerified(true);
            userRepository.save(user);
        }
        return token.getChannel();
    }

    public boolean isDevOrTestProfile() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.isEmpty() || profiles.contains("dev") || profiles.contains("test");
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String prefix = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return prefix + "****@" + parts[1];
    }

    public static String maskPhone(String phone) {
        if (phone == null) return "***";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) return "***";
        return "••••" + digits.substring(digits.length() - 4);
    }

    public record OtpDispatchResult(
            OtpChannel channel,
            OtpPurpose purpose,
            String maskedTarget,
            String providerLabel,
            boolean simulated,
            String devPreviewCode,
            int expiryMinutes
    ) {
    }
}
