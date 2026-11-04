#!/usr/bin/env bash
# Copia de ~/proyecto/init-db.sh. Carga kinet_bd.sql en Oracle (usuarios, tablas, catálogos y datos de prueba)
# reemplazando las claves de desarrollo por las del .env: en producción nunca quedan las claves del script.
# ATENCIÓN: borra y recrea los usuarios KINET_*. Solo en la primera instalación (después: db/migraciones).
# Uso (desde ~/proyecto, con el servicio db ya "healthy"):  bash init-db.sh
set -euo pipefail

set -a; source .env; set +a

SQL=/tmp/kinet_bd.sql
sed -e "s/KinetApp2026/${KINET_APP_PASSWORD}/g" \
    -e "s/KinetLectura2026/${KINET_READER_PASSWORD}/g" \
    -e "s/KinetAdmin2026/${KINET_ADMIN_PASSWORD}/g" \
    back/kinet_bd.sql > /tmp/kinet_bd_local.sql

docker compose cp /tmp/kinet_bd_local.sql db:$SQL
rm -f /tmp/kinet_bd_local.sql

docker compose exec -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 db \
  sh -c "sqlplus -s system/\"\$ORACLE_PASSWORD\"@localhost/FREEPDB1 @$SQL; rm -f $SQL"

echo "Base de datos lista. Ahora ejecuta: docker compose up -d --build"
