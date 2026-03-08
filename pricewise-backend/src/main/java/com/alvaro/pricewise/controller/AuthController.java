package com.alvaro.pricewise.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.auth.AuthDTOs.*;
import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Usuario registrado exitosamente"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login exitoso"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserProfileResponse response = authService.getProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/create-employee")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AuthResponse>> createEmployee(
            @AuthenticationPrincipal @org.springframework.lang.NonNull UserPrincipal userPrincipal,
            @Valid @RequestBody @org.springframework.lang.NonNull CreateEmployeeRequest request
    ) {
        AuthResponse response = authService.createEmployee(userPrincipal.getCompanyId(), userPrincipal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Empleado creado exitosamente"));
    }
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal @org.springframework.lang.NonNull UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(userPrincipal.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada exitosamente"));
    }

    // ─── Google OAuth2 ─────────────────────────

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        GoogleLoginResponse response = authService.googleLogin(request);
        String message = "AUTHENTICATED".equals(response.getStatus())
                ? "Login con Google exitoso"
                : "Usuario nuevo, requiere configuracion de empresa";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/google/complete-new-company")
    public ResponseEntity<ApiResponse<AuthResponse>> googleCompleteNewCompany(
            @Valid @RequestBody GoogleCompleteNewCompanyRequest request
    ) {
        AuthResponse response = authService.googleCompleteNewCompany(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Empresa y usuario creados exitosamente"));
    }

    @PostMapping("/google/complete-join")
    public ResponseEntity<ApiResponse<AuthResponse>> googleCompleteJoin(
            @Valid @RequestBody GoogleCompleteJoinRequest request
    ) {
        AuthResponse response = authService.googleCompleteJoin(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Usuario creado y unido a la empresa exitosamente"));
    }
}
