package com.alvaro.pricewise.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.auth.AuthDTOs.AuthResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.GoogleCompleteJoinRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.GoogleCompleteNewCompanyRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.GoogleLoginRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.GoogleLoginResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.UserRepository;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio dedicado a los flujos de autenticacion con Google OAuth2.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenService googleTokenService;

    /**
     * Primer paso de Google Sign-In: valida token y comprueba si el usuario ya existe.
     */
    @Transactional(readOnly = true)
    public GoogleLoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleTokenService.verify(request.getIdToken());
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        log.debug("Google login para: {}", email);

        return userRepository.findByEmail(email)
                .map(user -> {
                    UserPrincipal userPrincipal = UserPrincipal.create(user);
                    String token = jwtService.generateToken(userPrincipal);
                    return GoogleLoginResponse.authenticated(AuthResponse.of(
                            token,
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getRole().name(),
                            user.getCompany().getId(),
                            user.getCompany().getName()
                    ));
                })
                .orElseGet(() -> {
                    log.debug("Usuario Google no encontrado, requiere setup: {}", email);
                    return GoogleLoginResponse.needsSetup(email, name != null ? name : email.split("@")[0]);
                });
    }

    /**
     * Completa Google Sign-In creando una nueva empresa (usuario sera COMPANY_ADMIN).
     */
    @Transactional
    public AuthResponse googleCompleteNewCompany(GoogleCompleteNewCompanyRequest request) {
        GoogleIdToken.Payload payload = googleTokenService.verify(request.getGoogleIdToken());
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("El email ya esta registrado");
        }

        String username = generateUniqueUsername(name != null ? name : email.split("@")[0]);

        Company company = Company.builder()
                .name(request.getCompanyName())
                .businessType(request.getBusinessType())
                .build();
        company = companyRepository.save(company);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .company(company)
                .role(User.Role.COMPANY_ADMIN)
                .authProvider(User.AuthProvider.GOOGLE)
                .active(true)
                .build();
        user = userRepository.save(user);

        log.debug("Google: empresa {} creada con admin {} ({})", company.getCompanyCode(), user.getId(), email);

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = jwtService.generateToken(userPrincipal);

        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().name(), company.getId(), company.getName());
    }

    /**
     * Completa Google Sign-In uniendose a una empresa existente (usuario sera EMPLOYEE).
     */
    @Transactional
    public AuthResponse googleCompleteJoin(GoogleCompleteJoinRequest request) {
        GoogleIdToken.Payload payload = googleTokenService.verify(request.getGoogleIdToken());
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("El email ya esta registrado");
        }

        Company company = companyRepository.findByCompanyCode(request.getCompanyCode().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Codigo de empresa no valido"));

        if (!company.getActive()) {
            throw new BadRequestException("La empresa no esta activa");
        }

        String username = generateUniqueUsername(name != null ? name : email.split("@")[0]);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .company(company)
                .role(User.Role.EMPLOYEE)
                .authProvider(User.AuthProvider.GOOGLE)
                .active(true)
                .build();
        user = userRepository.save(user);

        log.debug("Google: usuario {} unido a empresa {} ({})", user.getId(), company.getCompanyCode(), email);

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = jwtService.generateToken(userPrincipal);

        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().name(), company.getId(), company.getName());
    }

    /**
     * Genera un username unico basado en el nombre de Google.
     */
    private String generateUniqueUsername(String baseName) {
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (sanitized.length() < 3) {
            sanitized = sanitized + "user";
        }
        if (sanitized.length() > 45) {
            sanitized = sanitized.substring(0, 45);
        }

        String candidate = sanitized;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = sanitized + suffix;
            suffix++;
            if (suffix > 1000) {
                throw new BadRequestException("No se pudo generar un nombre de usuario único");
            }
        }
        return candidate;
    }
}
