package com.alvaro.pricewise.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PriceWise API")
                        .version("1.0.0")
                        .description("""
                                API REST para el sistema de comparación y optimización de precios PriceWise.
                                
                                ## Funcionalidades principales:
                                - **Autenticación**: Registro y login con JWT
                                - **Productos**: CRUD completo de productos
                                - **Scraping**: Monitoreo automático de precios de competencia
                                - **Recomendaciones**: Sugerencias de pricing basadas en análisis
                                - **Alertas**: Notificaciones de cambios significativos
                                
                                ## Autenticación
                                La API usa JWT (JSON Web Tokens). Incluye el token en el header:
                                ```
                                Authorization: Bearer <tu_token>
                                ```
                                """)
                        .contact(new Contact()
                                .name("Alvaro")
                                .email("alvaro@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de desarrollo"),
                        new Server()
                                .url("https://pricewise-api.railway.app")
                                .description("Servidor de producción")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Introduce el token JWT")
                        ));
    }
}
