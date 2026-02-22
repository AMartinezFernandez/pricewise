package com.alvaro.pricewise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.AlertSummary;
import com.alvaro.pricewise.entity.Alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private EmailService emailService;

    public void sendAlert(Alert alert) {
        if (alert.getProduct().getCompany() == null) {
            return;
        }

        Long companyId = alert.getProduct().getCompany().getId();
        AlertSummary summary = AlertSummary.fromEntity(alert);

        String destination = "/topic/company/" + companyId + "/alerts";
        messagingTemplate.convertAndSend(destination, summary);

        log.debug("Alerta enviada a websocket: {}", destination);

        // Enviar email para alertas CRITICAL
        if (alert.getSeverity() == Alert.Severity.CRITICAL && emailService != null) {
            try {
                emailService.sendCriticalAlertEmail(alert);
            } catch (Exception e) {
                log.error("Error enviando email CRITICAL: {}", e.getMessage());
            }
        }
    }
}
