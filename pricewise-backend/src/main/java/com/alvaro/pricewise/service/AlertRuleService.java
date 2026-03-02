package com.alvaro.pricewise.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.AlertRuleResponse;
import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.CreateAlertRuleRequest;
import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.UpdateAlertRuleRequest;
import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.AlertRule;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.AlertRuleRepository;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> getRules(Long companyId) {
        return alertRuleRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(AlertRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlertRuleResponse createRule(Long companyId, CreateAlertRuleRequest request) {
        // Validar tipo de alerta
        Alert.AlertType alertType;
        try {
            alertType = Alert.AlertType.valueOf(request.getAlertType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de alerta no válido: " + request.getAlertType());
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // Validar producto si se especifica
        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .filter(p -> p.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        }

        AlertRule rule = AlertRule.builder()
                .company(company)
                .product(product)
                .alertType(alertType)
                .name(request.getName())
                .threshold(request.getThreshold())
                .targetPrice(request.getTargetPrice())
                .enabled(true)
                .build();

        AlertRule saved = alertRuleRepository.save(rule);
        log.debug("Regla de alerta creada: {} (ID: {})", alertType, saved.getId());
        return AlertRuleResponse.fromEntity(saved);
    }

    @Transactional
    public AlertRuleResponse updateRule(Long companyId, Long ruleId, UpdateAlertRuleRequest request) {
        AlertRule rule = alertRuleRepository.findByIdAndCompanyId(ruleId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de alerta no encontrada"));

        if (request.getThreshold() != null) {
            rule.setThreshold(request.getThreshold());
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getName() != null) {
            rule.setName(request.getName());
        }
        if (request.getTargetPrice() != null) {
            rule.setTargetPrice(request.getTargetPrice());
        }

        AlertRule saved = alertRuleRepository.save(rule);
        log.debug("Regla de alerta actualizada: {}", saved.getId());
        return AlertRuleResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteRule(Long companyId, Long ruleId) {
        AlertRule rule = alertRuleRepository.findByIdAndCompanyId(ruleId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de alerta no encontrada"));
        alertRuleRepository.delete(rule);
        log.debug("Regla de alerta eliminada: {}", ruleId);
    }

    @Transactional
    public AlertRuleResponse toggleRule(Long companyId, Long ruleId) {
        AlertRule rule = alertRuleRepository.findByIdAndCompanyId(ruleId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de alerta no encontrada"));
        rule.setEnabled(!rule.getEnabled());
        AlertRule saved = alertRuleRepository.save(rule);
        log.debug("Regla de alerta toggled: {} -> enabled={}", ruleId, saved.getEnabled());
        return AlertRuleResponse.fromEntity(saved);
    }
}
