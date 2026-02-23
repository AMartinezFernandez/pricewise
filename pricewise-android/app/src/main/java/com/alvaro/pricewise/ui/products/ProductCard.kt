package com.alvaro.pricewise.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.ui.common.formatPrice

@Composable
fun ProductCard(
    product: ProductResponse,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Columna izquierda: nombre, categoría, SKU
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                if (!product.category.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!product.asin.isNullOrBlank() || !product.sku.isNullOrBlank()) {
                    Text(
                        text = "ASIN: ${product.asin ?: product.sku}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Columna derecha: precio, margen, icono monitor
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(product.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (product.id == -1L) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onAddClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Añadir", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    if (product.margin != null) {
                        val marginColor = when {
                            product.margin >= 20 -> MaterialTheme.colorScheme.secondary
                            product.margin >= 0  -> MaterialTheme.colorScheme.onSurfaceVariant
                            else                 -> MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = "${"%.1f".format(product.margin)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = marginColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (product.monitoringEnabled) {
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Monitorización activa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
