-- =====================================================================
-- Kinet · Migración de producción del 04/11/2026 (respuesta a la auditoría del 03/11/2026)
--
-- NO borra datos. Se ejecuta UNA vez sobre la base que ya está en producción, como SYSTEM en el PDB:
--   docker exec -i kinet-db sqlplus -s system/"$ORACLE_PASSWORD"@localhost/FREEPDB1 @/tmp/2026-11-04_auditoria.sql
--
-- Antes de correrlo, reemplaza los tres valores <...> por contraseñas nuevas (no las publiques en el repo)
-- y pon la de KINET_APP también en DB_CONTRASENA del .env del servidor.
--
-- Qué hace:
--   1. Crea la bitácora AUDITORIA_ADMIN (criterio 6.7.9) con sus permisos de solo inserción.
--   2. Cambia las contraseñas de los usuarios de base de datos que estaban publicadas (NC-05).
--   3. Desactiva las actividades que no tienen reglas de puntuación vigentes (caso «Dyo»).
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

-- 2. Contraseñas nuevas (las anteriores estaban en el README del repositorio)
ALTER USER kinet_app    IDENTIFIED BY "<NUEVA_CLAVE_KINET_APP>";
ALTER USER kinet_admin  IDENTIFIED BY "<NUEVA_CLAVE_KINET_ADMIN>";
ALTER USER kinet_reader IDENTIFIED BY "<NUEVA_CLAVE_KINET_READER>";

-- 3. Actividades activas sin reglas vigentes: se desactivan (el jugador no ganaría XP con ellas)
UPDATE kinet_owner.TIPO_ACTIVIDAD t
SET    t.estado = 'INACTIVA'
WHERE  t.estado = 'ACTIVA'
AND    NOT EXISTS (SELECT 1 FROM kinet_owner.REGLA_PUNTUACION r
                   WHERE r.id_tipo_actividad = t.id_tipo_actividad
                   AND   r.vigente_desde <= TRUNC(SYSDATE)
                   AND  (r.vigente_hasta IS NULL OR r.vigente_hasta >= TRUNC(SYSDATE)));
COMMIT;

ALTER SESSION SET CURRENT_SCHEMA = system;
PROMPT === Migración 2026-11-04 aplicada: bitácora creada, contraseñas cambiadas, actividades sin reglas desactivadas ===
EXIT
