# Mejoras futuras

Líneas de evolución para iteraciones posteriores al MVP. Se priorizan por impacto en producto y por viabilidad técnica sobre la base actual.

## 1. Notificaciones push en tiempo real

Integración de WebSocket en el backend y notificaciones push en el cliente Android (Firebase Cloud Messaging). Hoy las alertas requieren abrir la app para verse. Con esto el usuario recibe el aviso en el momento en el que el análisis detecta una caída de precio o una oportunidad, sin esperar a refrescar.

## 2. Soporte de más competidores

Añadir scraping y normalización de precios para Idealo, eBay, PcComponentes y otros marketplaces relevantes en España. La arquitectura ya separa `Competitor` de `CompetitorPrice`, así que se trataría de implementar un servicio por proveedor reutilizando el modelo actual. Multiplica el valor del producto frente a la versión actual centrada en Amazon.

## 3. Reportes exportables

Generación de informes en PDF y Excel con la evolución de precios, márgenes y alertas por rango de fechas. Para una PYME es la diferencia entre tener datos en pantalla y poder llevarlos a una reunión de dirección o a un proveedor para negociar.

## 4. Doble factor para cuentas administrativas

2FA por TOTP (Google Authenticator, Authy) en `ADMIN` y `COMPANY_ADMIN`. Estas cuentas pueden modificar precios, crear empresas y borrar usuarios. El coste de implementación es bajo y el riesgo evitado es alto.

## 5. Caché distribuida con Redis

Sustituir el caché simple en memoria por Redis. Necesario en cuanto el backend se despliegue en más de una instancia, ya que ahora cada nodo tendría su propio caché y los datos quedarían inconsistentes. También permite cachear respuestas de Keepa y reducir consumo de su rate limit.

## 6. Internacionalización

Soporte multi-idioma (`es`, `en`, `ca`) en backend y app. Mensajes de error, plantillas de alerta y textos de UI separados a archivos de recursos. Abre la puerta a clientes fuera del mercado español sin cambios estructurales.
