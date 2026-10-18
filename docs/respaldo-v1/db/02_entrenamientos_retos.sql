-- =====================================================================
-- Kinet - Proceso 2: Gestión de Entrenamientos y Retos
-- Ejecutar DESPUÉS del script de usuarios, conectado con el usuario KINET.
-- =====================================================================

-- ---------------------------------------------------------------------
-- TIPO_ACTIVIDAD: catálogo de actividades y XP que da cada minuto
-- ---------------------------------------------------------------------
CREATE TABLE TIPO_ACTIVIDAD (
    id_tipo        NUMBER GENERATED ALWAYS AS IDENTITY,
    codigo         VARCHAR2(20) NOT NULL,
    nombre         VARCHAR2(50) NOT NULL,
    xp_por_minuto  NUMBER(3) NOT NULL,
    CONSTRAINT pk_tipo_actividad PRIMARY KEY (id_tipo),
    CONSTRAINT uq_tipo_actividad_codigo UNIQUE (codigo),
    CONSTRAINT ck_tipo_actividad_xp CHECK (xp_por_minuto BETWEEN 1 AND 20)
);

-- ---------------------------------------------------------------------
-- PROGRESO_JUGADOR: XP, nivel y racha acumulados (uno por usuario)
-- ---------------------------------------------------------------------
CREATE TABLE PROGRESO_JUGADOR (
    id_progreso                 NUMBER GENERATED ALWAYS AS IDENTITY,
    id_usuario                  NUMBER NOT NULL,
    xp_total                    NUMBER(10) DEFAULT 0 NOT NULL,
    nivel                       NUMBER(4) DEFAULT 1 NOT NULL,
    racha_actual                NUMBER(5) DEFAULT 0 NOT NULL,
    racha_maxima                NUMBER(5) DEFAULT 0 NOT NULL,
    fecha_ultimo_entrenamiento  DATE,
    CONSTRAINT pk_progreso_jugador PRIMARY KEY (id_progreso),
    CONSTRAINT uq_progreso_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_progreso_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_progreso_valores CHECK (xp_total >= 0 AND nivel >= 1 AND racha_actual >= 0 AND racha_maxima >= 0)
);

-- ---------------------------------------------------------------------
-- ENTRENAMIENTO: sesiones reportadas, validadas o rechazadas por el anti-trampa
-- ---------------------------------------------------------------------
CREATE TABLE ENTRENAMIENTO (
    id_entrenamiento  NUMBER GENERATED ALWAYS AS IDENTITY,
    id_usuario        NUMBER NOT NULL,
    id_tipo           NUMBER NOT NULL,
    duracion_minutos  NUMBER(4) NOT NULL,
    intensidad        VARCHAR2(10) NOT NULL,
    fecha_inicio      TIMESTAMP NOT NULL,
    fecha_fin         TIMESTAMP NOT NULL,
    estado            VARCHAR2(12) NOT NULL,
    motivo_rechazo    VARCHAR2(200),
    xp_otorgado       NUMBER(6) DEFAULT 0 NOT NULL,
    fecha_registro    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_entrenamiento PRIMARY KEY (id_entrenamiento),
    CONSTRAINT fk_entrenamiento_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_entrenamiento_tipo FOREIGN KEY (id_tipo) REFERENCES TIPO_ACTIVIDAD (id_tipo),
    CONSTRAINT ck_entrenamiento_intensidad CHECK (intensidad IN ('BAJA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_entrenamiento_estado CHECK (estado IN ('VALIDADO', 'RECHAZADO')),
    CONSTRAINT ck_entrenamiento_duracion CHECK (duracion_minutos > 0),
    CONSTRAINT ck_entrenamiento_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE INDEX ix_entrenamiento_usuario_fecha ON ENTRENAMIENTO (id_usuario, fecha_inicio);
CREATE INDEX ix_entrenamiento_tipo ON ENTRENAMIENTO (id_tipo);

-- ---------------------------------------------------------------------
-- RETO: catálogo de retos y misiones
-- id_tipo NULL = cuenta cualquier actividad. nivel_requerido NULL = todos los niveles.
-- ---------------------------------------------------------------------
CREATE TABLE RETO (
    id_reto          NUMBER GENERATED ALWAYS AS IDENTITY,
    titulo           VARCHAR2(100) NOT NULL,
    descripcion      VARCHAR2(300) NOT NULL,
    tipo_meta        VARCHAR2(10) NOT NULL,
    meta_valor       NUMBER(5) NOT NULL,
    id_tipo          NUMBER,
    nivel_requerido  VARCHAR2(20),
    duracion_dias    NUMBER(3) NOT NULL,
    xp_recompensa    NUMBER(5) NOT NULL,
    activo           CHAR(1) DEFAULT 'S' NOT NULL,
    CONSTRAINT pk_reto PRIMARY KEY (id_reto),
    CONSTRAINT fk_reto_tipo FOREIGN KEY (id_tipo) REFERENCES TIPO_ACTIVIDAD (id_tipo),
    CONSTRAINT ck_reto_tipo_meta CHECK (tipo_meta IN ('MINUTOS', 'SESIONES')),
    CONSTRAINT ck_reto_nivel CHECK (nivel_requerido IN ('INICIACION', 'INTERMEDIO', 'AVANZADO')),
    CONSTRAINT ck_reto_valores CHECK (meta_valor > 0 AND duracion_dias > 0 AND xp_recompensa > 0),
    CONSTRAINT ck_reto_activo CHECK (activo IN ('S', 'N'))
);

-- ---------------------------------------------------------------------
-- PARTICIPACION_RETO: avance de cada jugador en cada reto
-- ---------------------------------------------------------------------
CREATE TABLE PARTICIPACION_RETO (
    id_participacion  NUMBER GENERATED ALWAYS AS IDENTITY,
    id_reto           NUMBER NOT NULL,
    id_usuario        NUMBER NOT NULL,
    fecha_inicio      TIMESTAMP NOT NULL,
    fecha_limite      TIMESTAMP NOT NULL,
    progreso          NUMBER(6) DEFAULT 0 NOT NULL,
    estado            VARCHAR2(12) DEFAULT 'EN_CURSO' NOT NULL,
    fecha_completado  TIMESTAMP,
    CONSTRAINT pk_participacion_reto PRIMARY KEY (id_participacion),
    CONSTRAINT fk_participacion_reto FOREIGN KEY (id_reto) REFERENCES RETO (id_reto),
    CONSTRAINT fk_participacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_participacion_estado CHECK (estado IN ('EN_CURSO', 'COMPLETADO', 'VENCIDO'))
);

CREATE INDEX ix_participacion_usuario ON PARTICIPACION_RETO (id_usuario, estado);
CREATE INDEX ix_participacion_reto ON PARTICIPACION_RETO (id_reto);

-- ---------------------------------------------------------------------
-- NOTIFICACION: bandeja de avisos del jugador
-- ---------------------------------------------------------------------
CREATE TABLE NOTIFICACION (
    id_notificacion  NUMBER GENERATED ALWAYS AS IDENTITY,
    id_usuario       NUMBER NOT NULL,
    tipo             VARCHAR2(30) NOT NULL,
    mensaje          VARCHAR2(300) NOT NULL,
    leida            CHAR(1) DEFAULT 'N' NOT NULL,
    fecha            TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_notificacion PRIMARY KEY (id_notificacion),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_notificacion_leida CHECK (leida IN ('S', 'N'))
);

CREATE INDEX ix_notificacion_usuario ON NOTIFICACION (id_usuario, fecha);

-- =====================================================================
-- Datos iniciales
-- =====================================================================

-- Actividades (el administrador puede ajustar el XP por minuto)
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('RUNNING', 'Running', 2);
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('CICLISMO', 'Ciclismo', 1);
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('NATACION', 'Natación', 2);
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('GIMNASIO', 'Gimnasio', 2);
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('CALISTENIA', 'Calistenia', 2);
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('OTRO', 'Otra actividad', 1);

-- Retos para todos los niveles
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Primer paso', 'Registra tu primer entrenamiento en los próximos 3 días.', 'SESIONES', 1, NULL, NULL, 3, 30);
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Misión diaria', 'Entrena 30 minutos hoy.', 'MINUTOS', 30, NULL, NULL, 1, 20);
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Corredor de la semana', 'Suma 90 minutos de running en 7 días.', 'MINUTOS', 90,
        (SELECT id_tipo FROM TIPO_ACTIVIDAD WHERE codigo = 'RUNNING'), NULL, 7, 80);

-- Iniciación
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Arranque', 'Suma 60 minutos de entrenamiento en una semana.', 'MINUTOS', 60, NULL, 'INICIACION', 7, 50);
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Constancia', 'Completa 3 sesiones en una semana.', 'SESIONES', 3, NULL, 'INICIACION', 7, 60);

-- Intermedio
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('150 minutos activos', 'Alcanza los 150 minutos semanales que recomienda la OMS.', 'MINUTOS', 150, NULL, 'INTERMEDIO', 7, 90);
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Ritmo competitivo', 'Completa 4 sesiones en una semana.', 'SESIONES', 4, NULL, 'INTERMEDIO', 7, 100);

-- Avanzado
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Modo élite', 'Suma 300 minutos de entrenamiento en una semana.', 'MINUTOS', 300, NULL, 'AVANZADO', 7, 160);
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa)
VALUES ('Sin descanso', 'Completa 6 sesiones en una semana.', 'SESIONES', 6, NULL, 'AVANZADO', 7, 170);

-- Administrador de la plataforma (contraseña: Admin2026). Cambiarla después de la entrega.
INSERT INTO USUARIO (nombre, correo, contrasena_hash, id_rol, acepta_datos, fecha_acepta_datos)
VALUES ('Administrador Kinet', 'admin@kinet.com',
        '$2a$10$mpAUEkHYLGy2Ne9fRYy.BObxcaWrcUXAYTtwWrRHyphhvVio/ZSz.',
        (SELECT id_rol FROM ROL WHERE nombre = 'ADMINISTRADOR'), 'S', SYSTIMESTAMP);
INSERT INTO PERFIL (id_usuario, nickname)
VALUES ((SELECT id_usuario FROM USUARIO WHERE correo = 'admin@kinet.com'), 'admin_kinet');

COMMIT;

-- ---------------------------------------------------------------------
-- Para borrar el Proceso 2 y volver a empezar (solo en desarrollo):
-- DELETE FROM PERFIL WHERE nickname = 'admin_kinet';
-- DELETE FROM USUARIO WHERE correo = 'admin@kinet.com';
-- DROP TABLE NOTIFICACION;
-- DROP TABLE PARTICIPACION_RETO;
-- DROP TABLE RETO;
-- DROP TABLE ENTRENAMIENTO;
-- DROP TABLE PROGRESO_JUGADOR;
-- DROP TABLE TIPO_ACTIVIDAD;
-- ---------------------------------------------------------------------
