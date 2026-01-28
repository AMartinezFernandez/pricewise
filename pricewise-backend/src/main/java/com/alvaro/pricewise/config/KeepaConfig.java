package com.alvaro.pricewise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de Keepa API.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "keepa")
public class KeepaConfig {

    private String apiKey;
    private String defaultLocale = "ES";
    private int historyDays = 90;
    private int rateLimit = 10;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
