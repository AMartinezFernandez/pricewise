package com.alvaro.pricewise.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alvaro.pricewise.entity.Alert;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pricewise.email.enabled", havingValue = "true")
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${pricewise.email.from:noreply@pricewise.app}")
    private String fromAddress;

    @Async("taskExecutor")
    public void sendCriticalAlertEmail(Alert alert) {
        if (alert.getUser() == null || alert.getUser().getEmail() == null) {
            log.warn("No se puede enviar email: alerta {} sin usuario/email asociado", alert.getId());
            return;
        }

        String to = alert.getUser().getEmail();
        String productName = alert.getProduct() != null ? alert.getProduct().getName() : "Desconocido";
        String subject = "Alerta CRITICAL: " + productName;

        String body = buildHtmlBody(alert, productName);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Email CRITICAL enviado a {} para producto: {}", to, productName);
        } catch (MessagingException e) {
            log.error("Error enviando email CRITICAL a {}: {}", to, e.getMessage());
        }
    }

    private String buildHtmlBody(Alert alert, String productName) {
        String previousPrice = alert.getPreviousPrice() != null
                ? alert.getPreviousPrice().toPlainString() + " EUR" : "N/A";
        String newPrice = alert.getNewPrice() != null
                ? alert.getNewPrice().toPlainString() + " EUR" : "N/A";
        String changePercent = alert.getChangePercent() != null
                ? alert.getChangePercent().toPlainString() + "%" : "N/A";

        return """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <div style="background-color: #dc3545; color: white; padding: 16px; border-radius: 8px 8px 0 0;">
                    <h2 style="margin: 0;">Alerta CRITICAL</h2>
                    <p style="margin: 4px 0 0 0;">%s</p>
                  </div>
                  <div style="border: 1px solid #dee2e6; border-top: none; padding: 20px; border-radius: 0 0 8px 8px;">
                    <h3 style="color: #333;">%s</h3>
                    <p style="color: #666;">%s</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-top: 12px;">
                      <tr>
                        <td style="padding: 8px; border-bottom: 1px solid #eee; color: #666;">Precio anterior</td>
                        <td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: bold; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px; border-bottom: 1px solid #eee; color: #666;">Precio nuevo</td>
                        <td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: bold; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px; color: #666;">Variacion</td>
                        <td style="padding: 8px; font-weight: bold; text-align: right; color: #dc3545;">%s</td>
                      </tr>
                    </table>
                    <p style="margin-top: 20px; font-size: 12px; color: #999;">
                      Este email fue generado automaticamente por PriceWise.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                alert.getTitle(),
                productName,
                alert.getMessage() != null ? alert.getMessage() : "",
                previousPrice,
                newPrice,
                changePercent
        );
    }
}
