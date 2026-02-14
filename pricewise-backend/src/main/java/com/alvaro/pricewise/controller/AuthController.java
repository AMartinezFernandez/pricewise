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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de registro, login y perfil de usuario")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea una nueva cuenta de usuario y devuelve un token JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Usuario registrado exitosamente"));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un token JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login exitoso"));
    }

    @GetMapping("/profile")
    @Operation(summary = "Obtener perfil", description = "Devuelve la información del usuario autenticado")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserProfileResponse response = authService.getProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/create-employee")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    @Operation(summary = "Crear empleado", 
               description = "Crea un empleado dentro de la empresa del admin autenticado. Solo COMPANY_ADMIN y ADMIN.")
    public ResponseEntity<ApiResponse<AuthResponse>> createEmployee(
            @AuthenticationPrincipal @org.springframework.lang.NonNull UserPrincipal userPrincipal,
            @Valid @RequestBody @org.springframework.lang.NonNull CreateEmployeeRequest request
    ) {
        AuthResponse response = authService.createEmployee(userPrincipal.getCompanyId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Empleado creado exitosamente"));
    }
    @PostMapping("/change-password")
    @Operation(summary = "Cambiar contraseña", description = "Permite al usuario autenticado cambiar su contraseña")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal @org.springframework.lang.NonNull UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(userPrincipal.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada exitosamente"));
    }
}
