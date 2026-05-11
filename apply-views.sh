#!/bin/bash
# =======================================================================
# CortaAi — Aplicador de Views Analíticas
#
# Execute APÓS o `docker compose up -d` e após todos os serviços
# estarem saudáveis (Hibernate já criou as tabelas).
#
# Uso:
#   chmod +x apply-views.sh
#   ./apply-views.sh
#
# Ou no servidor (ZimaOS):
#   bash ~/cortaai/repo/apply-views.sh
# =======================================================================

set -e

MYSQL_CONTAINER="${MYSQL_CONTAINER:-cortaai-mysql}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-cortaai_db_@secret}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "⏳ Aguardando MySQL estar pronto..."
until docker exec "$MYSQL_CONTAINER" mysqladmin ping -uroot "-p${MYSQL_ROOT_PASSWORD}" --silent 2>/dev/null; do
    sleep 2
done

echo "✅ MySQL pronto. Aplicando views analíticas..."
docker exec -i "$MYSQL_CONTAINER" mysql -uroot "-p${MYSQL_ROOT_PASSWORD}" < "${SCRIPT_DIR}/views.sql"

echo "🎉 Views aplicadas com sucesso!"
