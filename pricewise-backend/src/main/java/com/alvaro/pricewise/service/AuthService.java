package com.alvaro.pricewise.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.auth.AuthDTOs.AuthResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CompanyResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CreateCompanyRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CreateEmployeeRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.LoginRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.RegisterRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.UserProfileResponse;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de autenticacion y registro.
 * Al registrarse, el usuario se vincula a una empresa existente mediante su código.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registro público: el usuario proporciona un código de empresa para vincularse como EMPLOYEE.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registrando usuario: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya esta registrado");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya esta en uso");
        }

        // Buscar empresa por código
        Company company = companyRepository.findByCompanyCode(request.getCompanyCode().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Código de empresa no válido"));

        if (!company.getActive()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // Crear usuario como EMPLOYEE de la empresa
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .company(company)
                .role(User.Role.EMPLOYEE)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.debug("Usuario registrado: {} en empresa: {} (código: {})", 
                user.getId(), company.getId(), company.getCompanyCode());

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = jwtService.generateToken(userPrincipal);

        return AuthResponse.of(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                company.getId(),
                company.getName()
        );
    }

    public AuthResponse login(LoginRequest request) {
        log.debug("Login para: {}", request.getEmailOrUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmailOrUsername(),
                        request.getPassword()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        String token = jwtService.generateToken(userPrincipal);

        log.debug("Login exitoso: {}", user.getId());

        return AuthResponse.of(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getCompany().getId(),
                user.getCompany().getName()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        Company company = user.getCompany();

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .companyId(company.getId())
                .companyName(company.getName())
                .companyType(company.getBusinessType())
                .companyPlan(company.getPlan().name())
                .role(user.getRole().name())
                .totalProducts(productRepository.countByCompanyId(company.getId()))
                .build();
    }

    /**
     * Crea un empleado dentro de la empresa del COMPANY_ADMIN que llama a este método.
     * Solo COMPANY_ADMIN y ADMIN pueden crear empleados.
     */
    @Transactional
    public AuthResponse createEmployee(Long companyId, CreateEmployeeRequest request) {
        log.debug("Creando empleado para empresa: {}", companyId);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya esta registrado");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya esta en uso");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BadRequestException("Empresa no encontrada"));

        User employee = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .company(company)
                .role(User.Role.EMPLOYEE)
                .active(true)
                .build();

        employee = userRepository.save(employee);
        log.debug("Empleado creado: {} en empresa: {}", employee.getId(), company.getId());

        UserPrincipal userPrincipal = UserPrincipal.create(employee);
        String token = jwtService.generateToken(userPrincipal);

        return AuthResponse.of(
                token,
                employee.getId(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getRole().name(),
                company.getId(),
                company.getName()
        );
    }

    /**
     * Crea una empresa con su COMPANY_ADMIN. Solo el ADMIN general puede llamar a este método.
     * El código de empresa se genera automáticamente (UUID 8 caracteres).
     */
    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        log.debug("Creando empresa: {}", request.getName());

        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new BadRequestException("El email del administrador ya esta registrado");
        }

        if (userRepository.existsByUsername(request.getAdminUsername())) {
            throw new BadRequestException("El nombre de usuario del administrador ya esta en uso");
        }

        if (request.getTaxId() != null && companyRepository.existsByTaxId(request.getTaxId())) {
            throw new BadRequestException("El CIF/NIF ya está registrado");
        }

        // Crear empresa (el companyCode se genera automáticamente con @PrePersist)
        Company company = Company.builder()
                .name(request.getName())
                .businessType(request.getBusinessType())
                .taxId(request.getTaxId())
                .build();
        company = companyRepository.save(company);

        // Crear el COMPANY_ADMIN de la empresa
        User admin = User.builder()
                .username(request.getAdminUsername())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .company(company)
                .role(User.Role.COMPANY_ADMIN)
                .active(true)
                .build();
        admin = userRepository.save(admin);

        log.debug("Empresa creada: {} con código: {} y admin: {}", 
                company.getId(), company.getCompanyCode(), admin.getId());

        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .companyCode(company.getCompanyCode())
                .businessType(company.getBusinessType())
                .taxId(company.getTaxId())
                .plan(company.getPlan().name())
                .adminUsername(admin.getUsername())
                .build();
    }
}

