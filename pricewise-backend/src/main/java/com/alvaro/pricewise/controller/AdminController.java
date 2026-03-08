package com.alvaro.pricewise.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.admin.AdminDTOs.PasswordChangeRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.RoleChangeRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.StatusChangeRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UpdateUserRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UserDetail;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UserSummary;
import com.alvaro.pricewise.dto.admin.DashboardStatsDTO;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CompanyResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CreateCompanyRequest;
import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.service.AdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserSummary>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDetail>> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUser(userId)));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDetail>> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.updateUser(userId, request),
                "Usuario actualizado correctamente"));
    }

    @PutMapping("/users/{userId}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long userId,
            @RequestBody PasswordChangeRequest request) {
        adminService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada correctamente"));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<UserSummary>> changeUserRole(
            @PathVariable Long userId,
            @RequestBody RoleChangeRequest request) {
        UserSummary updated = adminService.changeUserRole(userId, request.role());
        return ResponseEntity.ok(ApiResponse.success(updated, "Rol actualizado a " + updated.role()));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserSummary>> changeUserStatus(
            @PathVariable Long userId,
            @RequestBody StatusChangeRequest request) {
        UserSummary updated = adminService.changeUserStatus(userId, request.active());
        String message = updated.active() ? "Usuario activado" : "Usuario desactivado";
        return ResponseEntity.ok(ApiResponse.success(updated, message));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado"));
    }

    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getAllCompanies() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllCompanies()));
    }

    @GetMapping("/companies/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCompany(companyId)));
    }

    @PostMapping("/companies")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminService.createCompany(request), "Empresa creada exitosamente"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats()));
    }
}
