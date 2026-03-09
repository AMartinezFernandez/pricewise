package com.alvaro.pricewise.ui.common

import java.util.Currency
import java.util.Locale

fun formatPrice(price: Double, currencyCode: String = "EUR"): String {
    val symbol = try {
        Currency.getInstance(currencyCode).symbol
    } catch (_: Exception) {
        currencyCode
    }
    return String.format(Locale.getDefault(), "%.2f %s", price, symbol)
}
