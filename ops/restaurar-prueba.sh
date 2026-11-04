#!/usr/bin/env bash
# =====================================================================
# Kinet · Prueba de restauración (auditoría 03/11/2026, NC-02)
#
# Importa un respaldo en un esquema APARTE (KINET_RESTAURADO) sin tocar KINET_OWNER, y compara cuántas filas
# tiene cada tabla en los dos esquemas. Si todo coincide, el respaldo sirve. Al final borra el esquema de prueba.
#
# Uso:  ./restaurar-prueba.sh ~/respaldos/kinet_20261104_0200.dmp.gz
#
# Para una restauración REAL (pérdida de datos), ver docs/RESPALDO.md: se importa sobre KINET_OWNER con el
# backend detenido.
# =====================================================================
set -euo pipefail

ARCHIVO="${1:?Uso: $0 <respaldo .dmp.gz>}"
ENV_FILE="${ENV_FILE:-$HOME/proyecto/.env}"
if [ -f "$ENV_FILE" ]; then
  set -a; . "$ENV_FILE"; set +a
fi
: "${ORACLE_PASSWORD:?Falta ORACLE_PASSWORD (clave de SYSTEM) en $ENV_FILE}"
CONTENEDOR_DB="${CONTENEDOR_DB:-proyecto-db-1}"
PDB="${PDB:-FREEPDB1}"
NOMBRE="$(basename "$ARCHIVO" .dmp.gz)"

sql() {
  docker exec -i -e ORACLE_PASSWORD "$CONTENEDOR_DB" bash -c "sqlplus -s system/\"\$ORACLE_PASSWORD\"@localhost/$PDB"
}

if [ -f "${ARCHIVO%.dmp.gz}.sha256" ]; then
  (cd "$(dirname "$ARCHIVO")" && sha256sum -c "$(basename "${ARCHIVO%.dmp.gz}.sha256")")
fi

DIR_DUMP="$(echo "SET HEADING OFF FEEDBACK OFF PAGESIZE 0
SELECT directory_path FROM dba_directories WHERE directory_name = 'DATA_PUMP_DIR';" | sql | tr -d '[:space:]')"

echo "[$(date '+%F %T')] Restaurando $NOMBRE en KINET_RESTAURADO"
gunzip -c "$ARCHIVO" > "/tmp/$NOMBRE.dmp"
docker cp "/tmp/$NOMBRE.dmp" "$CONTENEDOR_DB:$DIR_DUMP/$NOMBRE.dmp"
rm -f "/tmp/$NOMBRE.dmp"

# Esquema de prueba limpio
sql <<SQL
WHENEVER SQLERROR CONTINUE
DROP USER kinet_restaurado CASCADE;
EXIT
SQL

docker exec -e ORACLE_PASSWORD "$CONTENEDOR_DB" bash -c "impdp system/\"\$ORACLE_PASSWORD\"@localhost/$PDB \
  directory=DATA_PUMP_DIR dumpfile=$NOMBRE.dmp logfile=restaurar_$NOMBRE.log \
  remap_schema=KINET_OWNER:KINET_RESTAURADO exclude=GRANT" || true

# Comparación de filas por tabla: original frente a restaurado
sql <<'SQL'
SET SERVEROUTPUT ON SIZE UNLIMITED FEEDBACK OFF
DECLARE
  n_orig NUMBER; n_rest NUMBER; difs NUMBER := 0; tablas NUMBER := 0;
BEGIN
  FOR t IN (SELECT table_name FROM dba_tables WHERE owner = 'KINET_OWNER' ORDER BY table_name) LOOP
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM kinet_owner.' || t.table_name INTO n_orig;
    BEGIN
      EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM kinet_restaurado.' || t.table_name INTO n_rest;
    EXCEPTION WHEN OTHERS THEN n_rest := -1;
    END;
    tablas := tablas + 1;
    IF n_orig <> n_rest THEN difs := difs + 1; END IF;
    DBMS_OUTPUT.PUT_LINE(RPAD(t.table_name, 28) || LPAD(n_orig, 8) || LPAD(n_rest, 8) ||
                         CASE WHEN n_orig = n_rest THEN '  OK' ELSE '  DIFERENTE' END);
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('Tablas: ' || tablas || ' · con diferencias: ' || difs);
END;
/
DROP USER kinet_restaurado CASCADE;
EXIT
SQL

docker exec "$CONTENEDOR_DB" rm -f "$DIR_DUMP/$NOMBRE.dmp"
echo "[$(date '+%F %T')] Prueba de restauración terminada. Si todas las tablas dicen OK, el respaldo es válido."
echo "Nota: las diferencias en tablas que cambiaron después del respaldo (por ejemplo NOTIFICACION) son normales."
