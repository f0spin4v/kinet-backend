# Kinet · Respaldo y recuperación de la base de datos

Responde a la auditoría del 03/11/2026: NC-02 y criterios 6.5.7, 6.7.8, 6.10.3 y 6.10.4.

## 1. Política

| Qué | Cómo |
|---|---|
| Qué se respalda | El esquema completo `KINET_OWNER`: tablas, datos, índices, restricciones, vistas y permisos |
| Herramienta | Oracle Data Pump (`expdp`), consistente aunque la app esté en uso |
| Frecuencia | Diario a las 2:00 a.m. (hora del servidor), con `cron` |
| Retención | 7 días en el servidor (`~/respaldos`) |
| Copia fuera del servidor | Bucket S3 (variable `RESPALDO_S3`) y, cada semana, una descarga manual al equipo del responsable |
| Integridad | Cada respaldo lleva su `sha256`. El script falla si Data Pump no termina "successfully completed" |
| Prueba de restauración | Al configurar el respaldo y luego una vez por sprint, con `ops/restaurar-prueba.sh` |
| Responsable | Juan Pablo Mora (base de datos). Suplente: Andrés Ospina |
| Objetivos | Pérdida máxima de datos (RPO): 24 h. Tiempo para volver a operar (RTO): 1 h |

## 2. Instalación en el servidor (una sola vez)

```bash
cd ~/proyecto/back && git pull
chmod +x ops/respaldo.sh ops/restaurar-prueba.sh
mkdir -p ~/respaldos

# Probar a mano
./ops/respaldo.sh

# Programar: todos los días a las 2:00 a.m.
( crontab -l 2>/dev/null; echo "0 2 * * * $HOME/proyecto/back/ops/respaldo.sh >> $HOME/respaldos/respaldo.log 2>&1" ) | crontab -
crontab -l
```

Si el contenedor de Oracle no se llama `proyecto-db-1` (verlo con `docker ps`), agregar `CONTENEDOR_DB=<nombre>` al `.env`.

**Copia en S3 (fuera del servidor):** crear un bucket privado, darle a la instancia un rol de IAM con permiso `s3:PutObject` sobre ese bucket, instalar la CLI (`sudo apt install -y awscli`) y agregar al `.env`:
```
RESPALDO_S3=s3://<bucket>/diarios
```

**Copia manual al equipo** (PowerShell, desde Windows):
```
scp -i "C:\Users\fospi\Documents\Proyectos\Conexion\SSH_KINET.pem" ubuntu@16.58.42.198:~/respaldos/kinet_*.dmp.gz .
```

## 3. Prueba de restauración

Importa el respaldo en un esquema aparte (`KINET_RESTAURADO`), compara las filas de cada tabla con producción y lo borra. **No toca los datos reales.**
```bash
./ops/restaurar-prueba.sh ~/respaldos/kinet_<fecha>.dmp.gz
```
Resultado esperado: todas las tablas con `OK`. Se guarda la salida como evidencia y se registra en Jira.

## 4. Recuperación ante una pérdida de datos real

1. Registrar el incidente en Jira (etiqueta `incidente`).
2. Detener el backend para que nadie escriba: `docker compose stop backend`.
3. Elegir el último respaldo bueno y verificarlo: `sha256sum -c kinet_<fecha>.sha256`.
4. Copiarlo al contenedor e importarlo reemplazando las tablas:
   ```bash
   gunzip -c ~/respaldos/kinet_<fecha>.dmp.gz > /tmp/restaurar.dmp
   DIR=$(docker exec proyecto-db-1 bash -c "echo \"SET HEADING OFF FEEDBACK OFF
   SELECT directory_path FROM dba_directories WHERE directory_name='DATA_PUMP_DIR';\" | sqlplus -s system/\$ORACLE_PASSWORD@localhost/FREEPDB1" | tr -d '[:space:]')
   docker cp /tmp/restaurar.dmp proyecto-db-1:$DIR/restaurar.dmp
   docker exec -e ORACLE_PASSWORD proyecto-db-1 bash -c "impdp system/\$ORACLE_PASSWORD@localhost/FREEPDB1 directory=DATA_PUMP_DIR dumpfile=restaurar.dmp schemas=KINET_OWNER table_exists_action=replace"
   ```
5. Arrancar y verificar: `docker compose start backend` y `curl -s https://kinetapp.duckdns.org/api/sistema/health`.
6. Cerrar el incidente en Jira con la hora de inicio, la de fin y los datos que se perdieron (máximo 24 h).

## 5. Si se pierde el servidor completo

Crear una instancia nueva con `DESPLIEGUE.md`, sección 3. Restaurar el respaldo más reciente de S3 o del equipo del responsable (sección 4) y apuntar la IP elástica a la instancia nueva. El dominio de DuckDNS no cambia.
