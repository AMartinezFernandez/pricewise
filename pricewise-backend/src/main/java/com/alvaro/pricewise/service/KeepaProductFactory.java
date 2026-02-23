package com.alvaro.pricewise.service;

import java.math.BigDecimal;

import com.alvaro.pricewise.entity.Product;

/**
 * Fabrica de productos temporales para consultas a Keepa.
 */
public final class KeepaProductFactory {

    private KeepaProductFactory() {
    }

    public static Product createTemporaryProduct(String asin) {
        return Product.builder()
                .name("Consulta temporal - " + asin)
                .currentPrice(BigDecimal.ZERO)
                .build();
    }
}
