package com.alvaro.pricewise.ui.common

import java.util.Locale

fun formatPrice(price: Double): String =
    "%.2f €".format(price)

fun priorityLabel(priority: String): String = when (priority) {
    "URGENT" -> "Urgente"
    "HIGH"   -> "Alta"
    "MEDIUM" -> "Media"
    "LOW"    -> "Baja"
    else     -> priority
}

fun recommendationTypeLabel(type: String): String = when (type) {
    "PRICE_TOO_HIGH"  -> "Precio muy alto"
    "PRICE_TOO_LOW"   -> "Oportunidad de margen"
    "COMPETITOR_DROP" -> "Bajada del competidor"
    "COMPETITOR_RISE" -> "Subida del competidor"
    "OUT_OF_STOCK"    -> "Sin stock en Amazon"
    else              -> type
}

fun alertTypeLabel(type: String): String = when (type) {
    "COMPETITOR_PRICE_DROP"    -> "Bajada de precio del competidor"
    "COMPETITOR_PRICE_RISE"    -> "Subida de precio del competidor"
    "HIGH_MARGIN_OPPORTUNITY"  -> "Oportunidad de margen"
    "PRICE_MATCH_NEEDED"       -> "Precio no competitivo"
    "PRICE_BELOW_COST"         -> "Precio por debajo del coste"
    else                       -> type
}
