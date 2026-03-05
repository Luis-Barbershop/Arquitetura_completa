# ===== STAGE 1: Build =====
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml

COPY discovery-service/pom.xml discovery-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY barbershop-service/pom.xml barbershop-service/pom.xml
COPY schedule-service/pom.xml schedule-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml

RUN chmod +x mvnw && ./mvnw dependency:go-offline -pl product-service -am -q -B

COPY product-service/src product-service/src

RUN ./mvnw package -pl product-service -am -DskipTests -q -B

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/product-service/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", \
  "-XX:+UseSerialGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+TieredCompilation", \
  "-XX:TieredStopAtLevel=1", \
  "-Xss256k", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:ReservedCodeCacheSize=32m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
