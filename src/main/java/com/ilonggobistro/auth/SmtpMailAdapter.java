package com.ilonggobistro.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Production Gmail / SMTP adapter.
 * Activated automatically when MAIL_PROVIDER=SMTP and MAIL_USERNAME / MAIL_PASSWORD are configured.
 */
@Component
public class SmtpMailAdapter implements MailDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailAdapter.class);
    private final AuthNotificationProperties properties;

    public SmtpMailAdapter(AuthNotificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isSimulated() {
        return false;
    }

    @Override
    public String providerLabel() {
        return "Gmail SMTP";
    }

    @Override
    public void sendOtpEmail(String toEmail, String recipientName, String otpCode, OtpPurpose purpose, int expiryMinutes) {
        AuthNotificationProperties.Mail mailCfg = properties.getMail();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailCfg.getHost());
        sender.setPort(mailCfg.getPort());
        sender.setUsername(mailCfg.getUsername());
        sender.setPassword(mailCfg.getPassword());

        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.starttls.enable", "true");
        javaMailProps.put("mail.smtp.connectiontimeout", "5000");
        javaMailProps.put("mail.smtp.timeout", "5000");

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailCfg.getFrom() != null && !mailCfg.getFrom().isBlank() ? mailCfg.getFrom() : mailCfg.getUsername());
        msg.setTo(toEmail);
        msg.setSubject("Ilonggo Bistro Staff Portal — Your " + purpose.getLabel() + " Code: " + otpCode);
        msg.setText("""
                Hello %s,

                Your 6-digit verification code for %s on the Ilonggo Bistro Staff Portal is:

                    %s

                This code expires in %d minutes and can only be used once.
                If you did not request this code, please ignore this email and notify the manager.

                — Ilonggo Bistro · Burgos-Osmeña St, Barotac Nuevo, Iloilo
                """.formatted(recipientName, purpose.getLabel(), otpCode, expiryMinutes));

        sender.send(msg);
        log.info("Dispatched {} OTP email via SMTP to {}", purpose, toEmail);
    }
}
