#!/usr/bin/env bash
# =====================================================================
# Kinet · Respaldo diario de la base de datos (auditoría 03/11/2026, NC-02)
#
# Exporta el esquema KINET_OWNER con Data Pump (expdp) desde el contenedor de Oracle, lo comprime, lo deja
# en ~/respaldos y borra los de más de 7 días. Si RESPALDO_S3 está definido, sube además una copia a S3
# (fuera del servidor).
#
# Se programa con cron todos los días a las 2:00 a.m. (ver docs/RESPALDO.md):
#   0 2 * * * /home/ubuntu/proyecto/back/ops/respaldo.sh >> /home/ubuntu/respaldos/respaldo.log 2>&1
#
# Variables (se leen de ~/proyecto/.env si existe):
#   ORACLE_PASSWORD   clave de SYSTEM del contenedor (obligatoria)
#   CONTENEDOR_DB     nombre del contenedor de Oracle (por defecto: proyecto-db-1)
#   PDB               servicio de la base (por defecto: FREEPDB1)
#   RETENCION_DIAS    días que se guardan los respaldos locales (por defecto: 7)
#   RESPALDO_S3       opcional, por ejemplo s3://kinet-respaldos/diarios
# =====================================================================
set -euo pipefail

ENV_FILE="${ENV_FILE:-$HOME/proyecto/.env}"
if [ -f "$ENV_FILE" ]; then
  set -a; . "$ENV_FILE"; set +a
fi

: "${ORACLE_PASSWORD:?Falta ORACLE_PASSWORD (clave de SYSTEM) en $ENV_FILE}"
CONTENEDOR_DB="${CONTENEDOR_DB:-proyecto-db-1}"
PDB="${PDB:-FREEPDB1}"
RETENCION_DIAS="${RETENCION_DIAS:-7}"
DESTINO="${DESTINO:-$HOME/respaldos}"
FECHA="$(date +%Y%m%d_%H%M)"
NOMBRE="kinet_${FECHA}"

mkdir -p "$DESTINO"
echo "[$(date '+%F %T')] Inicio del respaldo $NOMBRE"

# Carpeta de Data Pump dentro del contenedor
DIR_DUMP="$(docker exec -e ORACLE_PASSWORD "$CONTENEDOR_DB" bash -c "echo \"SET HEADING OFF FEEDBACK OFF PAGESIZE 0
SELECT directory_path FROM dba_directories WHERE directory_name = 'DATA_PUMP_DIR';\" \
  | sqlplus -s system/\"\$ORACLE_PASSWORD\"@localhost/$PDB" | tr -d '[:space:]')"
if [ -z "$DIR_DUMP" ]; then
  echo "No se encontró DATA_PUMP_DIR en $PDB" >&2
  exit 1
fi

# Exportación del esquema completo (tablas, datos, índices, restricciones, vistas y permisos)
docker exec -e ORACLE_PASSWORD "$CONTENEDOR_DB" bash -c "expdp system/\"\$ORACLE_PASSWORD\"@localhost/$PDB \
  schemas=KINET_OWNER directory=DATA_PUMP_DIR dumpfile=$NOMBRE.dmp logfile=$NOMBRE.log reuse_dumpfiles=y"

# Se saca del contenedor, se comprime y se borra adentro
docker cp "$CONTENEDOR_DB:$DIR_DUMP/$NOMBRE.dmp" "$DESTINO/$NOMBRE.dmp"
docker cp "$CONTENEDOR_DB:$DIR_DUMP/$NOMBRE.log" "$DESTINO/$NOMBRE.log"
docker exec "$CONTENEDOR_DB" rm -f "$DIR_DUMP/$NOMBRE.dmp" "$DIR_DUMP/$NOMBRE.log"
gzip -f "$DESTINO/$NOMBRE.dmp"

# Verificación mínima: el log de Data Pump debe terminar sin errores
if ! grep -q "successfully completed" "$DESTINO/$NOMBRE.log"; then
  echo "El respaldo terminó con advertencias o errores: revisar $DESTINO/$NOMBRE.log" >&2
  exit 2
fi
sha256sum "$DESTINO/$NOMBRE.dmp.gz" > "$DESTINO/$NOMBRE.sha256"

# Copia fuera del servidor (opcional)
if [ -n "${RESPALDO_S3:-}" ]; then
  aws s3 cp "$DESTINO/$NOMBRE.dmp.gz" "$RESPALDO_S3/$NOMBRE.dmp.gz"
  aws s3 cp "$DESTINO/$NOMBRE.sha256" "$RESPALDO_S3/$NOMBRE.sha256"
  echo "Copia en $RESPALDO_S3"
fi

# Retención
find "$DESTINO" -name 'kinet_*' -type f -mtime +"$RETENCION_DIAS" -delete

echo "[$(date '+%F %T')] Respaldo listo: $DESTINO/$NOMBRE.dmp.gz ($(du -h "$DESTINO/$NOMBRE.dmp.gz" | cut -f1))"
