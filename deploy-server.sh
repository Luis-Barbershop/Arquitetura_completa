#!/bin/bash
# =============================================================
# Script de Deploy - CortaAi Server
# Executar no servidor em ~/cortaai/repo/
# =============================================================

set -e

REPO_DIR="/DATA/cortaai/repo"
ENV_FILE=".env.prod"
DC="docker compose --env-file $ENV_FILE"

cd "$REPO_DIR"

echo "=========================================="
echo "  CortaAi - Deploy no Servidor"
echo "=========================================="

# 0. Corrigir permissoes do Docker config.json
echo ""
echo "[0/8] Corrigindo permissoes do Docker config..."
DOCKER_CONFIG_DIR="/DATA/.docker"
if [ -d "$DOCKER_CONFIG_DIR" ]; then
    CURRENT_USER=$(whoami)
    sudo chown -R "$CURRENT_USER":"$CURRENT_USER" "$DOCKER_CONFIG_DIR"
    sudo chmod 700 "$DOCKER_CONFIG_DIR"
    [ -f "$DOCKER_CONFIG_DIR/config.json" ] && sudo chmod 600 "$DOCKER_CONFIG_DIR/config.json"
    echo "  OK - Permissoes Docker config corrigidas para: $CURRENT_USER"
else
    mkdir -p "$DOCKER_CONFIG_DIR"
    chmod 700 "$DOCKER_CONFIG_DIR"
    echo "  OK - Diretorio $DOCKER_CONFIG_DIR criado"
fi

# 1. Copiar docker-compose.server.yml como docker-compose.yml
echo ""
echo "[1/8] Configurando docker-compose para servidor..."
cp docker-compose.server.yml docker-compose.yml
echo "  OK - docker-compose.yml configurado"

# 2. Verificar .env.prod
echo ""
echo "[2/8] Verificando $ENV_FILE..."
if [ ! -f "$ENV_FILE" ]; then
    echo "  ERRO - Arquivo $ENV_FILE nao encontrado! Configure as variaveis de ambiente."
    exit 1
fi
echo "  OK - $ENV_FILE encontrado"

# 3. Build das imagens
echo ""
echo "[3/8] Construindo imagens Docker (JRE + JAR)..."
echo "  Aguarde, isso pode levar alguns minutos..."
$DC build --parallel
echo "  OK - Imagens construidas"

# 4. Subir infraestrutura
echo ""
echo "[4/8] Subindo infraestrutura (MySQL, RabbitMQ, Redis)..."
$DC up -d db rabbitmq redis
echo "  Aguardando servicos ficarem healthy..."
sleep 15

for svc in db rabbitmq redis; do
    echo -n "  $svc: "
    $DC ps $svc --format "{{.Status}}"
done

# 5. Criar notification_db se nao existir
echo ""
echo "[5/8] Garantindo que notification_db existe..."
MYSQL_ROOT_PWD="$(grep MYSQL_ROOT_PASSWORD $ENV_FILE | cut -d= -f2)"
docker exec cortaai-mysql mysql -uroot -p"$MYSQL_ROOT_PWD" \
    -e "CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null \
    && echo "  OK - notification_db OK" \
    || echo "  AVISO - MySQL pode ainda estar iniciando, tentaremos novamente..."

sleep 5
docker exec cortaai-mysql mysql -uroot -p"$MYSQL_ROOT_PWD" \
    -e "CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null \
    && echo "  OK - notification_db confirmado"

# 6. Subir Discovery Service
echo ""
echo "[6/8] Subindo Discovery Service..."
$DC up -d discovery
echo "  Aguardando Eureka iniciar (~30s)..."
sleep 40

echo -n "  discovery: "
$DC ps discovery --format "{{.Status}}"

# 7. Subir Gateway
echo ""
echo "[7/8] Subindo API Gateway..."
$DC up -d gateway
sleep 10

# 8. Subir microsservicos + frontend
echo ""
echo "[8/8] Subindo microsservicos e frontend..."
$DC up -d user-service barbershop-service schedule-service payment-service notification-service product-service frontend
echo "  Aguardando servicos iniciarem..."
sleep 30

# Status final
echo ""
echo "=========================================="
echo "  Status Final"
echo "=========================================="
$DC ps

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
echo "Para ver logs de um servico:"
echo "   docker compose --env-file .env.prod logs -f <service-name>"
echo ""
echo "Para ver todos os logs:"
echo "   docker compose --env-file .env.prod logs -f"
