# ===== STAGE 1: Build =====
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Copia o wrapper e POM pai primeiro (cache de dependências)
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml

# Copia apenas os POMs dos módulos (sem código) para resolver dependências
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY barbershop-service/pom.xml barbershop-service/pom.xml
COPY schedule-service/pom.xml schedule-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml

RUN chmod +x mvnw && ./mvnw dependency:go-offline -pl discovery-service -am -q -B

# Copia o código fonte do módulo
COPY discovery-service/src discovery-service/src

# Build do JAR (sem testes)
RUN ./mvnw package -pl discovery-service -am -DskipTests -q -B

# ===== STAGE 2: Runtime (JRE apenas ~60% menor que JDK) =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia apenas o JAR final
COPY --from=builder /build/discovery-service/target/*.jar app.jar

EXPOSE 8761

# JVM otimizada para containers com pouca RAM
ENTRYPOINT ["java", \
  "-XX:+UseSerialGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Xss256k", \
  "-XX:MaxMetaspaceSize=96m", \
  "-XX:ReservedCodeCacheSize=32m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
