package com.alvaro.pricewise.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.AlertRuleResponse;
import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.CreateAlertRuleRequest;
import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.UpdateAlertRuleRequest;
import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.AlertRuleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador CRUD de reglas de alerta configurables.
 */
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'EMPLOYEE', 'ADMIN')")
@SuppressWarnings("null")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertRuleResponse>>> getRules(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<AlertRuleResponse> rules = alertRuleService.getRules(userPrincipal.requireCompanyId());
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createRule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateAlertRuleRequest request) {
        AlertRuleResponse rule = alertRuleService.createRule(userPrincipal.requireCompanyId(), request);
        return ResponseEntity.ok(ApiResponse.success(rule, "Regla de alerta creada"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateRule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertRuleRequest request) {
        AlertRuleResponse rule = alertRuleService.updateRule(userPrincipal.requireCompanyId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(rule, "Regla de alerta actualizada"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        alertRuleService.deleteRule(userPrincipal.requireCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success("Regla de alerta eliminada"));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> toggleRule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        AlertRuleResponse rule = alertRuleService.toggleRule(userPrincipal.requireCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success(rule));
    }
}
