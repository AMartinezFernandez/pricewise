package com.alvaro.pricewise.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.dto.user.UserDTOs.UserSummaryDTO;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getUsers(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<UserSummaryDTO> result = userService.getUsersByRole(principal.getRole(), principal.getCompanyId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = userService.getUserCount(principal.getRole(), principal.getCompanyId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteUser(userId, principal.getRole(), principal.getId(), principal.getCompanyId());
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado"));
    }
}
