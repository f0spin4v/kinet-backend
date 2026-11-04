-- =====================================================================
-- Kinet · Migración de producción del 04/11/2026 (respuesta a la auditoría del 03/11/2026)
--
-- NO borra datos. Se ejecuta UNA vez sobre la base que ya está en producción, como SYSTEM en el PDB
-- (desde ~/proyecto, con un respaldo hecho antes: ops/respaldo.sh):
--   docker compose cp back/db/migraciones/2026-11-04_auditoria.sql db:/tmp/migracion.sql
--   docker compose exec db sh -c 'sqlplus -s system/"$ORACLE_PASSWORD"@localhost/FREEPDB1 @/tmp/migracion.sql'
--
-- Qué hace:
--   1. Crea la bitácora AUDITORIA_ADMIN (criterio 6.7.9) con sus permisos de solo inserción.
--   2. Desactiva las actividades que no tienen reglas de puntuación vigentes (caso «Dyo»).
--
-- Las claves de los usuarios KINET_* de producción NO son las de kinet_bd.sql: init-db.sh las reemplaza por
-- las del .env al instalar. Si alguna vez hay que rotarlas, ver docs/RESPALDO.md («Rotar claves»).
-- =====================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT FAILURE

ALTER SESSION SET CURRENT_SCHEMA = kinet_owner;

-- 1. Bitácora
CREATE TABLE AUDITORIA_ADMIN (
    id_auditoria        NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_actor            NUMBER(10) NOT NULL,
    correo_actor        VARCHAR2(120) NOT NULL,
    accion              VARCHAR2(20) NOT NULL,
    id_usuario_afectado NUMBER(10),
    correo_afectado     VARCHAR2(120),
    valor_anterior      VARCHAR2(200),
    valor_nuevo         VARCHAR2(200),
    fecha_accion        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_auditoria_admin PRIMARY KEY (id_auditoria),
    CONSTRAINT ck_auditoria_accion CHECK (accion IN ('CAMBIO_ESTADO', 'CAMBIO_ROL', 'CREACION_CUENTA', 'AJUSTE_XP'))
);
CREATE INDEX ix_auditoria_fecha ON AUDITORIA_ADMIN (fecha_accion);
CREATE INDEX ix_auditoria_afectado ON AUDITORIA_ADMIN (id_usuario_afectado);

GRANT SELECT, INSERT ON kinet_owner.AUDITORIA_ADMIN TO rol_kinet_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON kinet_owner.AUDITORIA_ADMIN TO rol_kinet_admin;
CREATE OR REPLACE SYNONYM kinet_admin.AUDITORIA_ADMIN FOR kinet_owner.AUDITORIA_ADMIN;

-- 2. Actividades activas sin reglas vigentes: se desactivan (el jugador no ganaría XP con ellas)
UPDATE kinet_owner.TIPO_ACTIVIDAD t
SET    t.estado = 'INACTIVA'
WHERE  t.estado = 'ACTIVA'
AND    NOT EXISTS (SELECT 1 FROM kinet_owner.REGLA_PUNTUACION r
                   WHERE r.id_tipo_actividad = t.id_tipo_actividad
                   AND   r.vigente_desde <= TRUNC(SYSDATE)
                   AND  (r.vigente_hasta IS NULL OR r.vigente_hasta >= TRUNC(SYSDATE)));
COMMIT;

ALTER SESSION SET CURRENT_SCHEMA = system;
PROMPT === Migración 2026-11-04 aplicada: bitácora creada, actividades sin reglas desactivadas ===
EXIT
