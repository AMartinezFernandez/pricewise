package com.alvaro.pricewise;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test básico de carga del contexto de Spring.
 */
@SpringBootTest
@ActiveProfiles("dev")
class PriceWiseApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto carga sin errores, el test pasa
    }
}
