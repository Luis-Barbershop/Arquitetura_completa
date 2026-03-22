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

RUN chmod +x mvnw \
 && rm -rf /root/.m2/repository/org/apache/tomcat/embed /root/.m2/repository/net/bytebuddy \
 && ./mvnw -U dependency:go-offline -pl barbershop-service -am -B

COPY barbershop-service/src barbershop-service/src

RUN ./mvnw -U clean package -pl barbershop-service -am -DskipTests -B \
 && JAR_PATH="$(find /build/barbershop-service/target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" \
 && test -n "$JAR_PATH" \
 && jar tf "$JAR_PATH" | grep -q 'BOOT-INF/lib/tomcat-embed-core-' \
 && jar tf "$JAR_PATH" | grep -q 'BOOT-INF/lib/byte-buddy-'

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/barbershop-service/target/barbershop-service-*.jar /app/

RUN JAR_PATH="$(find /app -maxdepth 1 -type f -name 'barbershop-service-*.jar' ! -name '*.original' | head -n 1)" \
 && mv "$JAR_PATH" /app/app.jar

EXPOSE 8082

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
