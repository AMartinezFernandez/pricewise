package com.alvaro.pricewise.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.AlertRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(String callerRole, Long callerCompanyId) {
        if ("ADMIN".equals(callerRole)) {
            return userRepository.findAll();
        }
        return userRepository.findByCompanyId(callerCompanyId);
    }

    @Transactional(readOnly = true)
    public long getUserCount(String callerRole, Long callerCompanyId) {
        if ("ADMIN".equals(callerRole)) {
            return userRepository.count();
        }
        return userRepository.countByCompanyId(callerCompanyId);
    }

    @Transactional
    public void deleteUser(Long userId, String callerRole, Long callerId, Long callerCompanyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!"ADMIN".equals(callerRole)) {
            if (user.getCompany() == null || !user.getCompany().getId().equals(callerCompanyId)) {
                throw new ResourceNotFoundException("Usuario no encontrado");
            }
            if (user.getId().equals(callerId)) {
                throw new BadRequestException("No puedes eliminarte a ti mismo");
            }
            if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.COMPANY_ADMIN) {
                throw new BadRequestException("No puedes eliminar a un administrador");
            }
        }

        alertRepository.nullifyUserForAlerts(userId);
        productRepository.nullifyCreatedByForUser(userId);
        userRepository.delete(user);
    }
}
