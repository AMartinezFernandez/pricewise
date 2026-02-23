package com.alvaro.pricewise.config;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.Company.PlanType;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.entity.User.Role;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            if (companyRepository.count() == 0) {
                log.info("Sembrando base de datos inicial...");

                // 1. Crear Empresa Admin
                Company adminCompany = new Company();
                adminCompany.setName("PriceWise Admin Corp");
                adminCompany.setCompanyCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                adminCompany.setBusinessType("TECHNOLOGY");
                adminCompany.setPlan(PlanType.ENTERPRISE);
                adminCompany.setSharedStockEnabled(true);
                adminCompany.setCreatedAt(LocalDateTime.now());
                adminCompany.setUpdatedAt(LocalDateTime.now());
                
                adminCompany = companyRepository.save(adminCompany);
                log.info("Empresa Admin creada con código: {}", adminCompany.getCompanyCode());

                // 2. Crear Usuario Admin
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@pricewise.io");
                admin.setPassword(passwordEncoder.encode("Admin123")); // Password conocida
                admin.setRole(Role.ADMIN);
                admin.setCompany(adminCompany);
                admin.setActive(true);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());

                userRepository.save(admin);
                log.info("Usuario ADMIN de desarrollo creado: admin@pricewise.io");
            } else {
                log.info("La base de datos ya contiene datos. Omitiendo seed inicial.");
            }

            // Semilla de datos de prueba adicionales (Si pides 3 empresas con 4 empleados)
            seedTestCompanies();
        };
    }

    private void seedTestCompanies() {
        log.info("Sembrando datos de prueba adicionales...");

        createCompanyIfNotExists("Tech Solutions", "TECH0001", "TECHNOLOGY");
        createCompanyIfNotExists("Global Retail", "RETAIL01", "RETAIL");
        createCompanyIfNotExists("Consulting Pro", "CONSULT1", "CONSULTING");
        
        log.info("Verificación de datos de prueba completada.");
    }

    private void createCompanyIfNotExists(String name, String code, String type) {
        if (companyRepository.existsByCompanyCode(code)) {
            log.info("La empresa {} ({}) ya existe. Omitiendo.", name, code);
            return;
        }
        createCompanyWithEmployees(name, code, type);
    }

    private void createCompanyWithEmployees(String name, String code, String type) {
        // Crear Empresa
        Company company = new Company();
        company.setName(name);
        company.setCompanyCode(code);
        company.setBusinessType(type);
        company.setPlan(PlanType.PRO);
        company.setSharedStockEnabled(true);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        
        company = companyRepository.save(company);
        log.info("Empresa creada: {} ({})", name, code);

        // Crear Admin de Empresa
        String domain = name.toLowerCase().replace(" ", "") + ".com";
        String safeCode = code.toLowerCase().replaceAll("[^a-z0-9]", "");
        createUser(company, "admin@" + domain, "admin_" + safeCode, Role.COMPANY_ADMIN);

        // Crear 3 Empleados
        for (int i = 1; i <= 3; i++) {
            createUser(company, "user" + i + "@" + domain, "user" + i + "_" + safeCode, Role.EMPLOYEE);
        }
    }

    private void createUser(Company company, String email, String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password1"));
        user.setRole(role);
        user.setCompany(company);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("  -> Usuario creado: {} ({}) - {}", username, email, role);
    }
}
