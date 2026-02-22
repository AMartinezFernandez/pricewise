package com.alvaro.pricewise.service;

import com.alvaro.pricewise.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private Company company;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("user@test.com")
                .password("encoded")
                .company(company)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Producto Test")
                .currentPrice(new BigDecimal("100.00"))
                .company(company)
                .createdBy(user)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Envía email para alerta CRITICAL si emailService está disponible")
    void shouldSendEmailForCriticalAlert() {
        // emailService is injected via @Autowired(required = false), simulate it being present
        ReflectionTestUtils.setField(notificationService, "emailService", emailService);

        Alert alert = Alert.builder()
                .id(1L)
                .user(user)
                .product(product)
                .alertType(Alert.AlertType.COMPETITOR_PRICE_DROP)
                .title("Alerta CRITICAL")
                .severity(Alert.Severity.CRITICAL)
                .build();

        notificationService.sendAlert(alert);

        verify(messagingTemplate).convertAndSend(eq("/topic/company/1/alerts"), any(Object.class));
        verify(emailService).sendCriticalAlertEmail(alert);
    }

    @Test
    @DisplayName("No envía email para alerta WARNING")
    void shouldNotSendEmailForWarningAlert() {
        ReflectionTestUtils.setField(notificationService, "emailService", emailService);

        Alert alert = Alert.builder()
                .id(2L)
                .user(user)
                .product(product)
                .alertType(Alert.AlertType.COMPETITOR_PRICE_RISE)
                .title("Alerta WARNING")
                .severity(Alert.Severity.WARNING)
                .build();

        notificationService.sendAlert(alert);

        verify(messagingTemplate).convertAndSend(eq("/topic/company/1/alerts"), any(Object.class));
        verify(emailService, never()).sendCriticalAlertEmail(any());
    }

    @Test
    @DisplayName("No envía email para alerta INFO")
    void shouldNotSendEmailForInfoAlert() {
        ReflectionTestUtils.setField(notificationService, "emailService", emailService);

        Alert alert = Alert.builder()
                .id(3L)
                .user(user)
                .product(product)
                .alertType(Alert.AlertType.HIGH_MARGIN_OPPORTUNITY)
                .title("Alerta INFO")
                .severity(Alert.Severity.INFO)
                .build();

        notificationService.sendAlert(alert);

        verify(messagingTemplate).convertAndSend(eq("/topic/company/1/alerts"), any(Object.class));
        verify(emailService, never()).sendCriticalAlertEmail(any());
    }

    @Test
    @DisplayName("No falla si emailService es null (SMTP no configurado)")
    void shouldNotFailWhenEmailServiceIsNull() {
        ReflectionTestUtils.setField(notificationService, "emailService", null);

        Alert alert = Alert.builder()
                .id(4L)
                .user(user)
                .product(product)
                .alertType(Alert.AlertType.COMPETITOR_PRICE_DROP)
                .title("Alerta CRITICAL sin email")
                .severity(Alert.Severity.CRITICAL)
                .build();

        // Should not throw
        notificationService.sendAlert(alert);

        verify(messagingTemplate).convertAndSend(eq("/topic/company/1/alerts"), any(Object.class));
    }
}
