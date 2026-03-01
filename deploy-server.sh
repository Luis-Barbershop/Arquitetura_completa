#!/bin/bash
# =============================================================
# Script de Deploy - CortaAi Server
# Executar no servidor em /DATA/cortaai/repo/
# =============================================================

set -e

REPO_DIR="/DATA/cortaai/repo"
cd "$REPO_DIR"

echo "=========================================="
echo "  CortaAi - Deploy no Servidor"
echo "=========================================="

# 1. Copiar docker-compose.server.yml como docker-compose.yml (local only)
echo ""
echo "[1/7] Configurando docker-compose para servidor..."
cp docker-compose.server.yml docker-compose.yml
echo "  ✅ docker-compose.yml configurado com portas do servidor"

# 2. Verificar .env
echo ""
echo "[2/7] Verificando .env..."
if [ ! -f .env ]; then
    echo "  ❌ Arquivo .env não encontrado! Copie .env.example e configure."
    exit 1
fi
echo "  ✅ .env encontrado"

# 3. Subir infraestrutura
echo ""
echo "[3/7] Subindo infraestrutura (MySQL, RabbitMQ, Redis)..."
docker compose up -d db rabbitmq redis
echo "  Aguardando serviços ficarem healthy..."
sleep 15

# Verificar se estão healthy
for svc in db rabbitmq redis; do
    echo -n "  $svc: "
    docker compose ps $svc --format "{{.Status}}"
done

# 4. Criar notification_db se não existir (MySQL já existente não roda init.sql)
echo ""
echo "[4/7] Garantindo que notification_db existe..."
docker exec cortaai-mysql mysql -uroot -p"$(grep MYSQL_ROOT_PASSWORD .env | cut -d= -f2)" \
    -e "CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null \
    && echo "  ✅ notification_db OK" \
    || echo "  ⚠️  MySQL pode ainda estar iniciando, tentaremos novamente..."

# Retry se falhou
sleep 5
docker exec cortaai-mysql mysql -uroot -p"$(grep MYSQL_ROOT_PASSWORD .env | cut -d= -f2)" \
    -e "CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null \
    && echo "  ✅ notification_db confirmado"

# 5. Subir Discovery Service
echo ""
echo "[5/7] Subindo Discovery Service..."
docker compose up -d discovery
echo "  Aguardando Eureka iniciar (pode levar ~60s)..."
sleep 60

echo -n "  discovery: "
docker compose ps discovery --format "{{.Status}}"

# 6. Subir Gateway
echo ""
echo "[6/7] Subindo API Gateway..."
docker compose up -d gateway
sleep 10

# 7. Subir microserviços de negócio + frontend
echo ""
echo "[7/7] Subindo microserviços e frontend..."
docker compose up -d user-service barbershop-service schedule-service payment-service notification-service product-service frontend
echo "  Aguardando serviços iniciarem..."
sleep 30

# Status final
echo ""
echo "=========================================="
echo "  Status Final"
echo "=========================================="
docker compose ps

echo ""
echo "=========================================="
echo "  Portas do Servidor"
echo "=========================================="
echo "  MySQL:         localhost:3307"
echo "  RabbitMQ:      localhost:5673 (AMQP) / localhost:15673 (Management)"
echo "  Redis:         localhost:6380"
echo "  Eureka:        http://localhost:8761"
echo "  API Gateway:   http://localhost:8082"
echo "  Frontend:      http://localhost:5173"
echo "=========================================="
echo ""
echo "📋 Para ver logs de um serviço:"
echo "   docker compose logs -f <service-name>"
echo ""
echo "📋 Para ver todos os logs:"
echo "   docker compose logs -f"
