package com.alvaro.pricewise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuracion de Keepa API.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "keepa")
public class KeepaConfig {

    private String defaultLocale = "ES";
    private int historyDays = 90;
    private int rateLimit = 10;
}
