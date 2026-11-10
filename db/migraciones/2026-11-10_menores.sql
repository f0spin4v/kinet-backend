-- =====================================================================
-- Kinet · Migración de producción del 10/11/2026: titulares de 14 a 17 años (RN-33, KAN-77)
--
-- NO borra datos. Se ejecuta UNA vez sobre la base de producción, como SYSTEM en el PDB, con un respaldo
-- hecho antes (ops/respaldo.sh), desde ~/proyecto:
--   docker compose cp back/db/migraciones/2026-11-10_menores.sql db:/tmp/menores.sql
--   docker compose exec db sh -c 'sqlplus -s system/"$ORACLE_PASSWORD"@localhost/FREEPDB1 @/tmp/menores.sql'
-- Hacerla ANTES de desplegar el backend nuevo: sin la tabla, registrar a un menor o gestionar su autorización
-- falla (el backend no crea tablas: ddl-auto=none).
--
-- Qué hace:
--   1. Agrega el estado PENDIENTE a USUARIO (menor que espera la autorización de su representante).
--   2. Agrega la acción AUTORIZACION_MENOR a la bitácora.
--   3. Crea AUTORIZACION_MENOR con permisos para el backend y el administrador (no para reportes: datos personales).
-- =====================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT FAILURE

ALTER SESSION SET CURRENT_SCHEMA = kinet_owner;

-- 1. Estado PENDIENTE
ALTER TABLE USUARIO DROP CONSTRAINT ck_usuario_estado;
ALTER TABLE USUARIO ADD CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO', 'PENDIENTE'));

-- 2. Bitácora
ALTER TABLE AUDITORIA_ADMIN DROP CONSTRAINT ck_auditoria_accion;
ALTER TABLE AUDITORIA_ADMIN ADD CONSTRAINT ck_auditoria_accion CHECK (accion IN ('CAMBIO_ESTADO', 'CAMBIO_ROL',
    'CREACION_CUENTA', 'AJUSTE_XP', 'AUTORIZACION_MENOR'));

-- 3. Autorización del representante legal
CREATE TABLE AUTORIZACION_MENOR (
    id_autorizacion       NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario            NUMBER(10) NOT NULL,
    nombre_representante  VARCHAR2(160) NOT NULL,
    correo_representante  VARCHAR2(120) NOT NULL,
    parentesco            VARCHAR2(12) NOT NULL,
    estado                VARCHAR2(10) DEFAULT 'PENDIENTE' NOT NULL,
    canal                 VARCHAR2(13),
    token_hash            VARCHAR2(64),
    fecha_solicitud       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_expiracion      TIMESTAMP NOT NULL,
    fecha_respuesta       TIMESTAMP,
    soporte               VARCHAR2(300),
    id_admin              NUMBER(10),
    CONSTRAINT pk_autorizacion_menor PRIMARY KEY (id_autorizacion),
    CONSTRAINT uq_autorizacion_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_autorizacion_token UNIQUE (token_hash),
    CONSTRAINT fk_autorizacion_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_autorizacion_admin FOREIGN KEY (id_admin) REFERENCES USUARIO (id_usuario) ON DELETE SET NULL,
    CONSTRAINT ck_autorizacion_parentesco CHECK (parentesco IN ('MADRE', 'PADRE', 'TUTOR_LEGAL')),
    CONSTRAINT ck_autorizacion_estado CHECK (estado IN ('PENDIENTE', 'AUTORIZADA', 'RECHAZADA', 'REVOCADA')),
    CONSTRAINT ck_autorizacion_canal CHECK (canal IN ('CORREO', 'ADMINISTRADOR')),
    CONSTRAINT ck_autorizacion_respuesta CHECK (estado = 'PENDIENTE' OR fecha_respuesta IS NOT NULL),
    CONSTRAINT ck_autorizacion_soporte CHECK (canal IS NULL OR canal = 'CORREO' OR soporte IS NOT NULL),
    CONSTRAINT ck_autorizacion_correo_min CHECK (correo_representante = LOWER(correo_representante))
);

GRANT SELECT, INSERT, UPDATE, DELETE ON kinet_owner.AUTORIZACION_MENOR TO rol_kinet_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON kinet_owner.AUTORIZACION_MENOR TO rol_kinet_admin;
CREATE OR REPLACE SYNONYM kinet_admin.AUTORIZACION_MENOR FOR kinet_owner.AUTORIZACION_MENOR;

-- comprobación
SELECT table_name FROM dba_tables WHERE owner = 'KINET_OWNER' AND table_name = 'AUTORIZACION_MENOR';
EXIT
