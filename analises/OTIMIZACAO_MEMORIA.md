# 🧠 Otimização de Memória - CortaAí

## Resumo Executivo

Todas as otimizações foram aplicadas para **reduzir o consumo de RAM** dos microserviços Java de ~470-500MB para ~150-220MB cada, permitindo que todo o stack rode confortavelmente em um servidor com **12GB de RAM**.

---

## 🔴 Problemas Identificados

| Problema | Impacto na RAM |
|----------|---------------|
| `./mvnw spring-boot:run` no servidor | +150-200MB (Maven fica em memória) |
| `eclipse-temurin:17-jdk` como runtime | +200MB por container (JDK vs JRE) |
| Sem flags JVM de limitação de heap | JVM aloca 25% da RAM visível |
| `show-sql: true` em produção | Buffers de log desnecessários |
| HikariCP pool padrão (10 conexões) | ~10MB por pool desnecessário |
| Tomcat threads padrão (200) | ~1MB por thread = 200MB reservados |
| Sem `lazy-initialization` | Todas beans carregadas no startup |
| MySQL sem tuning de memória | Buffer pool padrão muito grande |

---

## ✅ Otimizações Aplicadas

### 1. Multi-Stage Dockerfiles (MAIOR IMPACTO: -300MB por container)

**Antes:** Cada serviço rodava com `eclipse-temurin:17-jdk` + `./mvnw spring-boot:run`
- JDK em memória: ~200MB
- Maven daemon em memória: ~150MB
- Compilação on-the-fly: +50MB
- **Total overhead: ~400MB por container**

**Depois:** Dockerfile multi-stage com build separado
- **Stage 1 (Build):** `eclipse-temurin:17-jdk-alpine` compila o JAR
- **Stage 2 (Runtime):** `eclipse-temurin:17-jre-alpine` roda apenas o JAR
- **Overhead removido: ~300-350MB por container**

### 2. JVM Flags Otimizadas para Containers

```
-XX:+UseSerialGC           → GC mais leve (menos threads auxiliares)
-XX:MaxRAMPercentage=75.0  → Usa até 75% do limite do container
-XX:+TieredCompilation     → Compilação JIT mais eficiente
-XX:TieredStopAtLevel=1    → Não faz otimizações C2 (economiza RAM)
-XX:+UseStringDeduplication → Remove Strings duplicadas da heap
-Xss256k                   → Stack menor (padrão 1MB por thread)
-XX:MaxMetaspaceSize=128m  → Limita Metaspace (classes carregadas)
-XX:ReservedCodeCacheSize=32m → Limita cache de código compilado
```

### 3. Spring Boot - application.yml

| Configuração | Antes | Depois | Economia |
|-------------|-------|--------|----------|
| `spring.main.lazy-initialization` | `false` | `true` | ~20-40MB |
| `show-sql` / `format-sql` | `true` | `false` | ~5MB buffers |
| Tomcat `threads.max` | 200 | 20 | ~180MB |
| Tomcat `threads.min-spare` | 10 | 2 | ~8MB |
| HikariCP `maximum-pool-size` | 10 | 5 | ~5MB |
| HikariCP `minimum-idle` | 10 | 2 | ~8MB |
| Hibernate `batch_size` | - | 10 | Reduz objetos em memória |

### 4. MySQL Tuning

```
--innodb-buffer-pool-size=128M   (padrão: 128M, explicitado)
--max-connections=50             (padrão: 151, reduzido)
--performance-schema=OFF         (economia: ~50-100MB)
--table-open-cache=200           (padrão: 4000, reduzido)
```

### 5. Redis & RabbitMQ

- **Redis:** `--maxmemory 32mb --maxmemory-policy allkeys-lru`
- **RabbitMQ:** `RABBITMQ_VM_MEMORY_HIGH_WATERMARK: 0.6`

---

## 📊 Comparação de Limites de Memória

### docker-compose.server.yml (Produção)

| Serviço | Antes | Depois | Economia |
|---------|-------|--------|----------|
| discovery-service | 300M | 200M | **-100M** |
| api-gateway | 512M | 300M | **-212M** |
| user-service | 512M | 300M | **-212M** |
| barbershop-service | 512M | 300M | **-212M** |
| schedule-service | 512M | 300M | **-212M** |
| payment-service | 512M | 300M | **-212M** |
| notification-service | 512M | 300M | **-212M** |
| product-service | 512M | 256M | **-256M** |
| MySQL | 512M | 384M | **-128M** |
| RabbitMQ | 256M | 192M | **-64M** |
| Redis | 64M | 48M | **-16M** |
| Frontend | 128M | 128M | 0 |
| **TOTAL** | **4,844M** | **3,008M** | **🎯 -1,836M** |

### Consumo Esperado vs Limite

| Serviço | Uso Esperado | Limite | % Uso |
|---------|-------------|--------|-------|
| discovery-service | ~120-150MB | 200M | ~65% |
| api-gateway | ~170-220MB | 300M | ~70% |
| user-service | ~170-220MB | 300M | ~70% |
| barbershop-service | ~160-200MB | 300M | ~65% |
| schedule-service | ~170-220MB | 300M | ~70% |
| payment-service | ~160-200MB | 300M | ~65% |
| notification-service | ~160-200MB | 300M | ~65% |
| product-service | ~140-180MB | 256M | ~65% |

---

## 📁 Arquivos Modificados

### Novos
- `backend/discovery-service/Dockerfile`
- `backend/api-gateway/Dockerfile`
- `backend/user-service/Dockerfile`
- `backend/barbershop-service/Dockerfile`
- `backend/schedule-service/Dockerfile`
- `backend/payment-service/Dockerfile`
- `backend/notification-service/Dockerfile`
- `backend/product-service/Dockerfile`
- `backend/.dockerignore`

### Modificados
- `docker-compose.server.yml` → Build com Dockerfile, limites reduzidos, tuning MySQL/Redis/RabbitMQ
- `docker-compose.yml` → JVM flags para desenvolvimento
- `deploy-server.sh` → Etapa de build adicionada
- `backend/*/src/main/resources/application.yml` → Todos os 8 serviços otimizados

---

## 🚀 Como Fazer Deploy

```bash
# No servidor ZimaOS
cd /DATA/cortaai/repo
git pull
./deploy-server.sh
```

O script agora faz `docker compose build --parallel` antes de subir os containers, construindo as imagens otimizadas com JRE.

---

## ⚠️ Notas Importantes

1. **Primeira build** será mais lenta (~5-10 min) pois precisa baixar dependências Maven dentro do Docker. Builds subsequentes usam cache.

2. **Se algum serviço reiniciar por OOM**, aumente o limite em incrementos de 50MB no `docker-compose.server.yml`.

3. **A flag `lazy-initialization: true`** faz o primeiro request a cada endpoint ser ligeiramente mais lento (50-100ms). Isso NÃO afeta requests subsequentes.

4. **`TieredStopAtLevel=1`** desabilita otimizações C2 do JIT. Para aplicações de baixa carga como esta, o impacto é imperceptível. Se notar latência alta em operações repetitivas, remova esta flag.

5. **`UseSerialGC`** é ideal para heaps pequenas (<256MB). Se aumentar o heap para >512MB no futuro, mude para `-XX:+UseG1GC`.
