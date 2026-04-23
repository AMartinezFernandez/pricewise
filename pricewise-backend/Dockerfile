# =========================================
# Dockerfile - PriceWise Backend
# Multi-stage build para imagen optimizada
# =========================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiar archivos de Maven primero (para cachear dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S pricewise && adduser -S pricewise -G pricewise
USER pricewise

# Copiar JAR desde stage de build
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto (valor por defecto; Railway inyecta PORT dinámico)
EXPOSE 9090

# Ejecutar con JVM optimizada para contenedores
# MaxRAMPercentage 50% deja margen para metaspace, threads y OS en contenedores pequeños
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=50.0", \
  "-XX:InitialRAMPercentage=25.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
