#!/bin/sh
echo "=== Redis keys ==="
docker exec cortaai-redis redis-cli KEYS "*availability*"

echo "=== Barbershops lat/lng ==="
docker exec cortaai-mysql mysql -uroot -proot cortaai_db -e "SELECT id, name, latitude, longitude FROM barbershops LIMIT 10"

echo "=== Schedule-service cache config ==="
docker exec schedule-service grep -r "CacheEvict\|Cacheable\|CachePut" /app 2>/dev/null || echo "nao encontrado via grep no container"
