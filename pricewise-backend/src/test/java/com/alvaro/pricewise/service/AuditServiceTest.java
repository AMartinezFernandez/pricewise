package com.alvaro.pricewise.service;

import com.alvaro.pricewise.entity.AuditLog;
import com.alvaro.pricewise.repository.AuditLogRepository;
import com.alvaro.pricewise.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private UserPrincipal buildPrincipal() {
        return UserPrincipal.builder()
                .id(1L)
                .companyId(1L)
                .username("admin")
                .email("admin@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("logAction guarda registro de auditoria con datos correctos")
    void logAction_SavesCorrectAuditLog() {
        UserPrincipal principal = buildPrincipal();

        auditService.logAction(principal, "CREATE_USER", "USER", 5L, "Nuevo usuario creado");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("admin", saved.getUsername());
        assertEquals(1L, saved.getCompanyId());
        assertEquals("CREATE_USER", saved.getAction());
        assertEquals("USER", saved.getEntityType());
        assertEquals(5L, saved.getEntityId());
        assertEquals("Nuevo usuario creado", saved.getDetails());
    }

    @Test
    @DisplayName("logAction con usuario null registra como SYSTEM")
    void logAction_NullUser_RegistersAsSystem() {
        auditService.logAction(null, "SYSTEM_TASK", "PRODUCT", 1L, "Tarea automatica");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertNull(saved.getUserId());
        assertEquals("SYSTEM", saved.getUsername());
        assertEquals("SYSTEM_TASK", saved.getAction());
    }
}
