# ===== STAGE 1: Build =====
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Copia os wrappers e o pom raiz
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml

# Copia os poms de todos os módulos para cache de dependências
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY barbershop-service/pom.xml barbershop-service/pom.xml
COPY schedule-service/pom.xml schedule-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml

RUN chmod +x mvnw \
 && ./mvnw dependency:go-offline -pl barbershop-service -am -B

# Copia o código fonte
COPY barbershop-service/src barbershop-service/src

# BUILD: Força o clean e o package. 
# O segredo aqui é o filtro no find para garantir que pegamos o JAR executável (maior que 1MB)
RUN ./mvnw clean package -pl barbershop-service -am -DskipTests -B \
 && JAR_PATH=$(find barbershop-service/target -maxdepth 1 -name "barbershop-service-*.jar" ! -name "*plain.jar" | head -n 1) \
 && cp "$JAR_PATH" /build/app.jar \
 && echo "JAR selecionado: $JAR_PATH" \
 && jar tf /build/app.jar | grep -q "BOOT-INF/classes/" || (echo "ERRO: JAR não é executável do Spring Boot" && exit 1)

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia apenas o arquivo final renomeado do estágio anterior
COPY --from=builder /build/app.jar app.jar

EXPOSE 8082

# Parâmetros de otimização de memória que você já definiu (muito bons por sinal)
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