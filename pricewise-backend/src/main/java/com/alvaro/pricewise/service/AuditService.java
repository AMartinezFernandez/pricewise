package com.alvaro.pricewise.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alvaro.pricewise.entity.AuditLog;
import com.alvaro.pricewise.repository.AuditLogRepository;
import com.alvaro.pricewise.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(UserPrincipal user, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getDisplayUsername() : "SYSTEM")
                .companyId(user != null ? user.getCompanyId() : null)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(getClientIp())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: {} {} {} (id={}) by {}", action, entityType, entityId,
                entityId, user != null ? user.getDisplayUsername() : "SYSTEM");
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<AuditLog> getAuditLogsByCompany(Long companyId, Pageable pageable) {
        return auditLogRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
    }

    public Page<AuditLog> getAuditLogsByCompanyAndAction(Long companyId, String action, Pageable pageable) {
        return auditLogRepository.findByCompanyIdAndActionOrderByCreatedAtDesc(companyId, action, pageable);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener IP del cliente: {}", e.getMessage());
        }
        return null;
    }
}
