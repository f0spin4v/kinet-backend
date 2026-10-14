-- =====================================================================
-- Kinet - Sprint 3
-- Proceso 1: Gestion de Usuario y Autenticacion
-- Base de datos: Oracle (12c o superior, probado para Oracle Free 23ai)
-- Ejecutar conectado con el usuario KINET (no con SYSTEM).
-- =====================================================================

-- ---------------------------------------------------------------------
-- ROL
-- ---------------------------------------------------------------------
CREATE TABLE ROL (
    id_rol  NUMBER GENERATED ALWAYS AS IDENTITY,
    nombre  VARCHAR2(30) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
);

-- ---------------------------------------------------------------------
-- USUARIO
-- correo unico (RNF-26), contrasena solo como hash BCrypt (RNF-21),
-- consentimiento de tratamiento de datos (Ley 1581 de 2012).
-- ---------------------------------------------------------------------
CREATE TABLE USUARIO (
    id_usuario          NUMBER GENERATED ALWAYS AS IDENTITY,
    nombre              VARCHAR2(100) NOT NULL,
    correo              VARCHAR2(150) NOT NULL,
    contrasena_hash     VARCHAR2(100) NOT NULL,
    id_rol              NUMBER NOT NULL,
    estado              VARCHAR2(15) DEFAULT 'ACTIVO' NOT NULL,
    acepta_datos        CHAR(1) DEFAULT 'N' NOT NULL,
    fecha_acepta_datos  TIMESTAMP,
    fecha_registro      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    ultimo_acceso       TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES ROL (id_rol),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'BLOQUEADO')),
    CONSTRAINT ck_usuario_acepta CHECK (acepta_datos IN ('S', 'N')),
    -- el correo se guarda siempre en minusculas para que la unicidad sea real
    CONSTRAINT ck_usuario_correo_min CHECK (correo = LOWER(correo))
);

CREATE INDEX ix_usuario_rol ON USUARIO (id_rol);

-- ---------------------------------------------------------------------
-- PERFIL (1 a 1 con USUARIO) - pantalla UI-002
-- ---------------------------------------------------------------------
CREATE TABLE PERFIL (
    id_perfil         NUMBER GENERATED ALWAYS AS IDENTITY,
    id_usuario        NUMBER NOT NULL,
    nickname          VARCHAR2(30) NOT NULL,
    avatar_url        VARCHAR2(300),
    fecha_nacimiento  DATE,
    ciudad            VARCHAR2(80),
    deporte_favorito  VARCHAR2(50),
    CONSTRAINT pk_perfil PRIMARY KEY (id_perfil),
    CONSTRAINT uq_perfil_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_perfil_nickname UNIQUE (nickname),
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id_usuario) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- TOKEN_RECUPERACION - recuperar contrasena por correo
-- Se guarda el hash del token, nunca el token en claro. Un solo uso.
-- ---------------------------------------------------------------------
CREATE TABLE TOKEN_RECUPERACION (
    id_token          NUMBER GENERATED ALWAYS AS IDENTITY,
    id_usuario        NUMBER NOT NULL,
    token_hash        VARCHAR2(100) NOT NULL,
    fecha_creacion    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_expiracion  TIMESTAMP NOT NULL,
    usado             CHAR(1) DEFAULT 'N' NOT NULL,
    CONSTRAINT pk_token_recuperacion PRIMARY KEY (id_token),
    CONSTRAINT uq_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_token_usado CHECK (usado IN ('S', 'N'))
);

CREATE INDEX ix_token_usuario ON TOKEN_RECUPERACION (id_usuario);

-- ---------------------------------------------------------------------
-- EVALUACION_NIVEL - pantalla UI-001 (Placement Match)
-- Guarda el resultado de la evaluacion inicial del jugador.
-- ---------------------------------------------------------------------
CREATE TABLE EVALUACION_NIVEL (
    id_evaluacion  NUMBER GENERATED ALWAYS AS IDENTITY,
    id_usuario     NUMBER NOT NULL,
    puntaje        NUMBER(5) NOT NULL,
    nivel_inicial  VARCHAR2(20) NOT NULL,
    fecha          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_evaluacion_nivel PRIMARY KEY (id_evaluacion),
    CONSTRAINT fk_evaluacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_evaluacion_puntaje CHECK (puntaje >= 0)
);

CREATE INDEX ix_evaluacion_usuario ON EVALUACION_NIVEL (id_usuario);

-- ---------------------------------------------------------------------
-- Datos iniciales
-- ---------------------------------------------------------------------
INSERT INTO ROL (nombre) VALUES ('JUGADOR');
INSERT INTO ROL (nombre) VALUES ('ADMINISTRADOR');

COMMIT;

-- ---------------------------------------------------------------------
-- Para borrar todo y volver a empezar (solo en desarrollo):
-- DROP TABLE EVALUACION_NIVEL;
-- DROP TABLE TOKEN_RECUPERACION;
-- DROP TABLE PERFIL;
-- DROP TABLE USUARIO;
-- DROP TABLE ROL;
-- ---------------------------------------------------------------------
