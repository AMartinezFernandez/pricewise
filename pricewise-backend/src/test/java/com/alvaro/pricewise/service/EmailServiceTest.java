package com.alvaro.pricewise.service;

import com.alvaro.pricewise.entity.*;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private Company company;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@pricewise.app");

        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .businessType("ecommerce")
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("user@test.com")
                .password("encoded")
                .company(company)
                .role(User.Role.COMPANY_ADMIN)
                .build();

        product = Product.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .sku("B012345678")
                .currentPrice(new BigDecimal("1199.00"))
                .company(company)
                .createdBy(user)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Envía email para alerta CRITICAL")
    void shouldSendEmailForCriticalAlert() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Alert alert = Alert.builder()
                .id(1L)
                .user(user)
                .product(product)
                .alertType(Alert.AlertType.COMPETITOR_PRICE_DROP)
                .title("Bajada de precio detectada")
                .message("El competidor ha bajado el precio un 15%")
                .previousPrice(new BigDecimal("1199.00"))
                .newPrice(new BigDecimal("1019.00"))
                .changePercent(new BigDecimal("15.01"))
                .severity(Alert.Severity.CRITICAL)
                .build();

        emailService.sendCriticalAlertEmail(alert);

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("No envía email si el usuario no tiene email")
    void shouldNotSendWhenUserHasNoEmail() {
        User noEmailUser = User.builder()
                .id(2L)
                .username("nomail")
                .email(null)
                .password("encoded")
                .company(company)
                .role(User.Role.EMPLOYEE)
                .build();

        Alert alert = Alert.builder()
                .id(2L)
                .user(noEmailUser)
                .product(product)
                .alertType(Alert.AlertType.COMPETITOR_PRICE_DROP)
                .title("Test")
                .severity(Alert.Severity.CRITICAL)
                .build();

        emailService.sendCriticalAlertEmail(alert);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("No envía email si la alerta no tiene usuario")
    void shouldNotSendWhenAlertHasNoUser() {
        Alert alert = Alert.builder()
                .id(3L)
                .user(null)
                .product(product)
                .alertType(Alert.AlertType.COMPETITOR_PRICE_DROP)
                .title("Test")
                .severity(Alert.Severity.CRITICAL)
                .build();

        emailService.sendCriticalAlertEmail(alert);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
