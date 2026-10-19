-- =====================================================================
-- Kinet - Script completo de base de datos (modelo v2)
-- Base de datos: Oracle 19c o superior (probado en Oracle Free 23ai)
-- Ejecutar como SYSTEM conectado al PDB, por ejemplo:
--   sqlplus system/<clave>@localhost:1522/FREEPDB1 @kinet_bd.sql
-- (en el contenedor kinet-db la clave de SYSTEM es la de ORACLE_PASSWORD).
--
-- ATENCIÓN: borra y vuelve a crear los usuarios de Kinet con todos sus datos. Es un script de desarrollo.
--
-- Contenido:
--   0. Limpieza (usuarios, roles y perfiles de Kinet; el script se puede repetir)
--   S. Seguridad: perfiles de contraseña, usuarios y roles de base de datos
--   1. Cuenta y seguridad: ROL, USUARIO, PERFIL_JUGADOR, TOKEN_RECUPERACION
--   2. Evaluación de nivel: NIVEL_HABILIDAD, PREGUNTA_EVALUACION, OPCION_EVALUACION,
--      EVALUACION_USUARIO, RESPUESTA_EVALUACION
--   3. Actividad y puntuación: TIPO_ACTIVIDAD, REGLA_PUNTUACION, NIVEL_PROGRESION,
--      ENTRENAMIENTO (cronómetro en el servidor), PLATAFORMA_DEPORTE (apps reconocidas),
--      EVIDENCIA_ENTRENAMIENTO (imagen o enlace verificado)
--   4. Competitivo: TEMPORADA, LIGA, RANGO, PARTICIPACION_TEMPORADA, HISTORIAL_RANGO, ENFRENTAMIENTO,
--      PARTICIPANTE_ENFRENTAMIENTO, SOLICITUD_MATCHMAKING, COMPETENCIA, PARTICIPANTE_COMPETENCIA
--   5. Social y retos: CLAN, MEMBRESIA_CLAN, RETO, PARTICIPACION_RETO, APORTE_RETO
--   6. Recompensas: LOGRO, LOGRO_USUARIO, MOVIMIENTO_XP
--   7. NOTIFICACION
--   V. Vistas para reportes: V_USUARIO_PUBLICO, V_RANKING_TEMPORADA
--   8. Datos iniciales (catálogos)
--   9. Datos de prueba
--   P. Permisos de los roles y sinónimos
--
-- Usuarios de base de datos (las claves son de desarrollo: cámbialas fuera de tu máquina):
--   KINET_OWNER   dueño de las tablas. Sin contraseña (NO AUTHENTICATION): nadie inicia sesión con él.
--   KINET_APP     el backend. Lee y escribe datos, pero no crea ni borra tablas. MOVIMIENTO_XP e
--                 HISTORIAL_RANGO son de solo inserción: el historial no se puede editar ni borrar.
--   KINET_READER  reportes. Solo lectura y sin acceso a credenciales ni datos personales
--                 (USUARIO, PERFIL_JUGADOR, TOKEN_RECUPERACION, EVIDENCIA_ENTRENAMIENTO); usa V_USUARIO_PUBLICO.
--   KINET_ADMIN   administrador funcional de los datos (lectura y escritura sobre todo el esquema).
--
-- Correcciones respecto al diagrama original (detalle en docs/MIGRACION_V2.md):
--   * PERFIL_JUGADOR.xp_total: total de XP acumulado (cache del libro MOVIMIENTO_XP).
--   * ENTRENAMIENTO: cronómetro en el servidor; una sola sesión EN_CURSO. Sin GPS ni wearables.
--   * EVIDENCIA_ENTRENAMIENTO: imagen o enlace a una app deportiva (PLATAFORMA_DEPORTE) con bonus de XP;
--     el enlace es obligatorio en sesiones de más de 180 min y para competir. Revisada por un entrenador.
--   * PARTICIPACION_TEMPORADA: puntos_actuales y estado; HISTORIAL_RANGO guarda cada cambio de rango.
--   * COMPETENCIA: duelos, competencias grupales y torneos por invitación (aceptar o rechazar).
--   * PARTICIPACION_RETO: único por (reto, usuario, periodo) para repetir retos diarios y semanales.
--   * NOTIFICACION: fecha_creacion para ordenar la bandeja y fecha_lectura en lugar de un S/N.
--   * MOVIMIENTO_XP: CHECK de coherencia entre el origen y su referencia.
--   * Índices únicos parciales para las reglas "solo uno a la vez".
--
-- Contraseñas de las cuentas de la aplicación (datos de prueba):
--   admin@kinet.com -> Admin2026
--   resto           -> Kinet2026
-- =====================================================================

SET DEFINE OFF

-- =====================================================================
-- 0. LIMPIEZA
-- Cierra las sesiones abiertas (por ejemplo, el backend) y borra usuarios, roles y perfiles de Kinet.
-- =====================================================================
BEGIN
    FOR u IN (SELECT username FROM dba_users
              WHERE username IN ('KINET_OWNER', 'KINET_APP', 'KINET_READER', 'KINET_ADMIN')) LOOP
        FOR ses IN (SELECT sid, serial# AS serie FROM v$session WHERE username = u.username) LOOP
            BEGIN
                EXECUTE IMMEDIATE 'ALTER SYSTEM KILL SESSION ''' || ses.sid || ',' || ses.serie || ''' IMMEDIATE';
            EXCEPTION
                WHEN OTHERS THEN NULL;
            END;
        END LOOP;
        EXECUTE IMMEDIATE 'DROP USER ' || u.username || ' CASCADE';
    END LOOP;
    FOR r IN (SELECT role FROM dba_roles
              WHERE role IN ('ROL_KINET_APP', 'ROL_KINET_READER', 'ROL_KINET_ADMIN')) LOOP
        EXECUTE IMMEDIATE 'DROP ROLE ' || r.role;
    END LOOP;
    FOR pf IN (SELECT DISTINCT profile FROM dba_profiles
               WHERE profile IN ('KINET_PERFIL_PERSONAS', 'KINET_PERFIL_SERVICIO')) LOOP
        EXECUTE IMMEDIATE 'DROP PROFILE ' || pf.profile || ' CASCADE';
    END LOOP;
END;
/


-- =====================================================================
-- S. SEGURIDAD: PERFILES, USUARIOS Y ROLES
-- =====================================================================

-- Personas (lectores y administradores): bloqueo tras 5 intentos fallidos durante una hora, la clave
-- caduca a los 180 días y no se puede repetir en un año ni entre las últimas 5.
CREATE PROFILE kinet_perfil_personas LIMIT
    FAILED_LOGIN_ATTEMPTS 5
    PASSWORD_LOCK_TIME    1/24
    PASSWORD_LIFE_TIME    180
    PASSWORD_GRACE_TIME   7
    PASSWORD_REUSE_TIME   365
    PASSWORD_REUSE_MAX    5
    SESSIONS_PER_USER     5
    IDLE_TIME             60;

-- Cuenta de servicio (backend): sin caducidad ni bloqueo por intentos, para que un atacante no pueda
-- dejar la aplicación sin base de datos bloqueando su cuenta. El límite de sesiones cubre el pool de conexiones.
CREATE PROFILE kinet_perfil_servicio LIMIT
    FAILED_LOGIN_ATTEMPTS UNLIMITED
    PASSWORD_LIFE_TIME    UNLIMITED
    SESSIONS_PER_USER     50;

-- Dueño del esquema: sin contraseña, nadie puede iniciar sesión con él.
CREATE USER kinet_owner NO AUTHENTICATION
    DEFAULT TABLESPACE users
    QUOTA UNLIMITED ON users;

CREATE USER kinet_app IDENTIFIED BY "KinetApp2026"
    DEFAULT TABLESPACE users
    PROFILE kinet_perfil_servicio;

CREATE USER kinet_reader IDENTIFIED BY "KinetLectura2026"
    DEFAULT TABLESPACE users
    PROFILE kinet_perfil_personas;

CREATE USER kinet_admin IDENTIFIED BY "KinetAdmin2026"
    DEFAULT TABLESPACE users
    PROFILE kinet_perfil_personas;

CREATE ROLE rol_kinet_app;
CREATE ROLE rol_kinet_reader;
CREATE ROLE rol_kinet_admin;
GRANT CREATE SESSION TO rol_kinet_app;
GRANT CREATE SESSION TO rol_kinet_reader;
GRANT CREATE SESSION TO rol_kinet_admin;
GRANT rol_kinet_app TO kinet_app;
GRANT rol_kinet_reader TO kinet_reader;
GRANT rol_kinet_admin TO kinet_admin;

-- Todo lo que sigue se crea en el esquema KINET_OWNER.
ALTER SESSION SET CURRENT_SCHEMA = kinet_owner;


-- =====================================================================
-- 1. CUENTA Y SEGURIDAD
-- =====================================================================

CREATE TABLE ROL (
    id_rol       NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre       VARCHAR2(30) NOT NULL,
    descripcion  VARCHAR2(200),
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    CONSTRAINT ck_rol_nombre CHECK (nombre IN ('JUGADOR', 'ADMINISTRADOR', 'ENTRENADOR'))
);

-- Contraseña solo como hash BCrypt (RNF-21). Consentimiento de datos: Ley 1581 de 2012.
CREATE TABLE USUARIO (
    id_usuario              NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_rol                  NUMBER(10) NOT NULL,
    nombres                 VARCHAR2(80) NOT NULL,
    apellidos               VARCHAR2(80) NOT NULL,
    correo                  VARCHAR2(120) NOT NULL,
    password_hash           VARCHAR2(255) NOT NULL,
    estado                  VARCHAR2(10) DEFAULT 'ACTIVO' NOT NULL,
    acepta_politica_datos   CHAR(1) DEFAULT 'N' NOT NULL,
    fecha_acepta_politica   DATE,
    fecha_registro          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES ROL (id_rol),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),
    CONSTRAINT ck_usuario_acepta CHECK (acepta_politica_datos IN ('S', 'N')),
    CONSTRAINT ck_usuario_politica CHECK (acepta_politica_datos = 'N' OR fecha_acepta_politica IS NOT NULL),
    CONSTRAINT ck_usuario_correo_min CHECK (correo = LOWER(correo))
);
CREATE INDEX ix_usuario_rol ON USUARIO (id_rol);

-- 1 a 1 con USUARIO. Guarda el estado de juego que mantiene el motor (rachas y XP acumulado).
CREATE TABLE PERFIL_JUGADOR (
    id_usuario              NUMBER(10) NOT NULL,
    gamertag                VARCHAR2(30) NOT NULL,
    fecha_nacimiento        DATE,
    avatar_url              VARCHAR2(300),
    xp_total                NUMBER(10) DEFAULT 0 NOT NULL,
    racha_actual            NUMBER(4) DEFAULT 0 NOT NULL,
    racha_maxima            NUMBER(4) DEFAULT 0 NOT NULL,
    fecha_ultima_actividad  DATE,
    CONSTRAINT pk_perfil_jugador PRIMARY KEY (id_usuario),
    CONSTRAINT uq_perfil_gamertag UNIQUE (gamertag),
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_perfil_valores CHECK (xp_total >= 0 AND racha_actual >= 0 AND racha_maxima >= racha_actual)
);

-- Enlace de un solo uso para restablecer la contraseña. Se guarda el hash SHA-256, nunca el token.
CREATE TABLE TOKEN_RECUPERACION (
    id_token          NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario        NUMBER(10) NOT NULL,
    token_hash        VARCHAR2(255) NOT NULL,
    fecha_creacion    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_expiracion  TIMESTAMP NOT NULL,
    fecha_uso         TIMESTAMP,
    CONSTRAINT pk_token_recuperacion PRIMARY KEY (id_token),
    CONSTRAINT uq_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_token_fechas CHECK (fecha_expiracion > fecha_creacion)
);
CREATE INDEX ix_token_usuario ON TOKEN_RECUPERACION (id_usuario);


-- =====================================================================
-- 2. EVALUACIÓN DE NIVEL (Placement Match)
-- Cada opción "vota" por un nivel con un peso (puntaje). El nivel resultante es el más votado;
-- en empate gana el de menor orden (criterio conservador).
-- =====================================================================

CREATE TABLE NIVEL_HABILIDAD (
    id_nivel_habilidad  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre              VARCHAR2(40) NOT NULL,
    orden               NUMBER(2) NOT NULL,
    descripcion         VARCHAR2(200),
    CONSTRAINT pk_nivel_habilidad PRIMARY KEY (id_nivel_habilidad),
    CONSTRAINT uq_nivel_habilidad_nombre UNIQUE (nombre),
    CONSTRAINT uq_nivel_habilidad_orden UNIQUE (orden)
);

CREATE TABLE PREGUNTA_EVALUACION (
    id_pregunta  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    enunciado    VARCHAR2(300) NOT NULL,
    orden        NUMBER(2) NOT NULL,
    activa       CHAR(1) DEFAULT 'S' NOT NULL,
    CONSTRAINT pk_pregunta_evaluacion PRIMARY KEY (id_pregunta),
    CONSTRAINT uq_pregunta_orden UNIQUE (orden),
    CONSTRAINT ck_pregunta_activa CHECK (activa IN ('S', 'N'))
);

CREATE TABLE OPCION_EVALUACION (
    id_opcion           NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_pregunta         NUMBER(10) NOT NULL,
    id_nivel_habilidad  NUMBER(10) NOT NULL,
    titulo              VARCHAR2(100) NOT NULL,
    descripcion         VARCHAR2(300),
    puntaje             NUMBER(3) NOT NULL,
    CONSTRAINT pk_opcion_evaluacion PRIMARY KEY (id_opcion),
    CONSTRAINT uq_opcion_pregunta_nivel UNIQUE (id_pregunta, id_nivel_habilidad),
    CONSTRAINT fk_opcion_pregunta FOREIGN KEY (id_pregunta) REFERENCES PREGUNTA_EVALUACION (id_pregunta) ON DELETE CASCADE,
    CONSTRAINT fk_opcion_nivel FOREIGN KEY (id_nivel_habilidad) REFERENCES NIVEL_HABILIDAD (id_nivel_habilidad),
    CONSTRAINT ck_opcion_puntaje CHECK (puntaje BETWEEN 1 AND 10)
);
CREATE INDEX ix_opcion_nivel ON OPCION_EVALUACION (id_nivel_habilidad);

-- Se puede repetir (cada 30 días). El nivel vigente es el de la evaluación más reciente.
CREATE TABLE EVALUACION_USUARIO (
    id_evaluacion       NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario          NUMBER(10) NOT NULL,
    id_nivel_habilidad  NUMBER(10) NOT NULL,
    version_algoritmo   VARCHAR2(20) NOT NULL,
    fecha_evaluacion    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_evaluacion_usuario PRIMARY KEY (id_evaluacion),
    CONSTRAINT fk_evaluacion_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_evaluacion_nivel FOREIGN KEY (id_nivel_habilidad) REFERENCES NIVEL_HABILIDAD (id_nivel_habilidad)
);
CREATE INDEX ix_evaluacion_usuario_fecha ON EVALUACION_USUARIO (id_usuario, fecha_evaluacion);

CREATE TABLE RESPUESTA_EVALUACION (
    id_evaluacion  NUMBER(10) NOT NULL,
    id_opcion      NUMBER(10) NOT NULL,
    CONSTRAINT pk_respuesta_evaluacion PRIMARY KEY (id_evaluacion, id_opcion),
    CONSTRAINT fk_respuesta_evaluacion FOREIGN KEY (id_evaluacion) REFERENCES EVALUACION_USUARIO (id_evaluacion) ON DELETE CASCADE,
    CONSTRAINT fk_respuesta_opcion FOREIGN KEY (id_opcion) REFERENCES OPCION_EVALUACION (id_opcion)
);
CREATE INDEX ix_respuesta_opcion ON RESPUESTA_EVALUACION (id_opcion);


-- =====================================================================
-- 3. ACTIVIDAD Y PUNTUACIÓN
-- =====================================================================

-- estado INACTIVA: no se puede iniciar un entrenamiento con ella, pero su historial se conserva.
CREATE TABLE TIPO_ACTIVIDAD (
    id_tipo_actividad  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre             VARCHAR2(40) NOT NULL,
    estado             VARCHAR2(10) DEFAULT 'ACTIVA' NOT NULL,
    CONSTRAINT pk_tipo_actividad PRIMARY KEY (id_tipo_actividad),
    CONSTRAINT uq_tipo_actividad_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipo_actividad_nombre CHECK (nombre = UPPER(nombre)),
    CONSTRAINT ck_tipo_actividad_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
);

-- XP por minuto según actividad e intensidad, con vigencia. vigente_hasta NULL = vigente.
CREATE TABLE REGLA_PUNTUACION (
    id_regla             NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_tipo_actividad    NUMBER(10) NOT NULL,
    intensidad           VARCHAR2(10) NOT NULL,
    xp_por_minuto        NUMBER(6,2) NOT NULL,
    duracion_minima_min  NUMBER(3) NOT NULL,
    vigente_desde        DATE NOT NULL,
    vigente_hasta        DATE,
    CONSTRAINT pk_regla_puntuacion PRIMARY KEY (id_regla),
    CONSTRAINT uq_regla_vigencia UNIQUE (id_tipo_actividad, intensidad, vigente_desde),
    CONSTRAINT fk_regla_tipo FOREIGN KEY (id_tipo_actividad) REFERENCES TIPO_ACTIVIDAD (id_tipo_actividad),
    CONSTRAINT ck_regla_intensidad CHECK (intensidad IN ('BAJA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_regla_valores CHECK (xp_por_minuto > 0 AND duracion_minima_min >= 1),
    CONSTRAINT ck_regla_vigencia CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde)
);

CREATE TABLE NIVEL_PROGRESION (
    id_nivel_progresion  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    numero_nivel         NUMBER(3) NOT NULL,
    xp_requerido         NUMBER(10) NOT NULL,
    CONSTRAINT pk_nivel_progresion PRIMARY KEY (id_nivel_progresion),
    CONSTRAINT uq_nivel_progresion_numero UNIQUE (numero_nivel),
    CONSTRAINT uq_nivel_progresion_xp UNIQUE (xp_requerido),
    CONSTRAINT ck_nivel_progresion_valores CHECK (numero_nivel >= 1 AND xp_requerido >= 0)
);

-- Sesión medida con el cronómetro de la app. "Iniciar" crea la fila EN_CURSO con la hora del servidor;
-- "Finalizar" guarda fin y duración (también hora del servidor) y la pasa por el anti-trampa:
-- VALIDADO, PENDIENTE (revisión de un entrenador) o RECHAZADO. El XP que dio queda en MOVIMIENTO_XP.
-- distancia_km, calorias_kcal, series, repeticiones y peso_kg los anota el jugador según la actividad (distancia en
-- running, ciclismo y natación; series, repeticiones y peso en fuerza y calistenia): son informativos, no dan XP
-- ni cuentan para retos.
CREATE TABLE ENTRENAMIENTO (
    id_entrenamiento      NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario            NUMBER(10) NOT NULL,
    id_tipo_actividad     NUMBER(10) NOT NULL,
    fecha_hora_inicio     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_hora_fin        TIMESTAMP,
    duracion_min          NUMBER(4),
    intensidad            VARCHAR2(10),
    distancia_km          NUMBER(7,2),
    calorias_kcal         NUMBER(6),
    series                NUMBER(3),
    repeticiones          NUMBER(4),
    peso_kg               NUMBER(6,2),
    notas                 VARCHAR2(500),
    estado_validacion     VARCHAR2(10) DEFAULT 'EN_CURSO' NOT NULL,
    motivo_rechazo        VARCHAR2(200),
    fecha_registro        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_entrenamiento PRIMARY KEY (id_entrenamiento),
    CONSTRAINT fk_entrenamiento_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_entrenamiento_tipo FOREIGN KEY (id_tipo_actividad) REFERENCES TIPO_ACTIVIDAD (id_tipo_actividad),
    CONSTRAINT ck_entrenamiento_intensidad CHECK (intensidad IS NULL OR intensidad IN ('BAJA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_entrenamiento_estado CHECK (estado_validacion IN ('EN_CURSO', 'PENDIENTE', 'VALIDADO', 'RECHAZADO')),
    -- en curso no hay fin ni duración; terminado, sí. Lo que da XP necesita intensidad.
    CONSTRAINT ck_entrenamiento_cronometro CHECK (
        (estado_validacion = 'EN_CURSO' AND fecha_hora_fin IS NULL AND duracion_min IS NULL)
        OR (estado_validacion <> 'EN_CURSO' AND fecha_hora_fin IS NOT NULL AND duracion_min IS NOT NULL)),
    CONSTRAINT ck_entrenamiento_intensidad_fin CHECK (estado_validacion NOT IN ('VALIDADO', 'PENDIENTE')
                                                      OR intensidad IS NOT NULL),
    CONSTRAINT ck_entrenamiento_rechazo CHECK (estado_validacion <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL),
    CONSTRAINT ck_entrenamiento_valores CHECK ((duracion_min IS NULL OR duracion_min >= 0)
                                               AND (distancia_km IS NULL OR distancia_km >= 0)
                                               AND (calorias_kcal IS NULL OR calorias_kcal >= 0)
                                               AND (series IS NULL OR series >= 1)
                                               AND (repeticiones IS NULL OR repeticiones >= 1)
                                               AND (peso_kg IS NULL OR peso_kg >= 0)),
    CONSTRAINT ck_entrenamiento_fechas CHECK (fecha_hora_fin IS NULL OR fecha_hora_fin > fecha_hora_inicio)
);
CREATE INDEX ix_entrenamiento_usuario_inicio ON ENTRENAMIENTO (id_usuario, fecha_hora_inicio);
CREATE INDEX ix_entrenamiento_tipo ON ENTRENAMIENTO (id_tipo_actividad);
CREATE INDEX ix_entrenamiento_estado ON ENTRENAMIENTO (estado_validacion);
-- un solo cronómetro en marcha por jugador
CREATE UNIQUE INDEX ux_entrenamiento_en_curso ON ENTRENAMIENTO (
    CASE WHEN estado_validacion = 'EN_CURSO' THEN id_usuario END);

-- Apps deportivas cuyos enlaces se aceptan como evidencia. El backend lee esta tabla para reconocer un enlace:
-- exige https, que el host sea "dominio" (o un subdominio, como www.) y que la ruta cumpla "patron_ruta"
-- (expresión regular). Agregar o apagar una app es un INSERT o un UPDATE; no hace falta tocar el código.
CREATE TABLE PLATAFORMA_DEPORTE (
    id_plataforma  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre         VARCHAR2(30) NOT NULL,
    dominio        VARCHAR2(100) NOT NULL,
    patron_ruta    VARCHAR2(200) NOT NULL,
    activa         CHAR(1) DEFAULT 'S' NOT NULL,
    CONSTRAINT pk_plataforma_deporte PRIMARY KEY (id_plataforma),
    CONSTRAINT uq_plataforma_dominio UNIQUE (dominio),
    CONSTRAINT ck_plataforma_nombre CHECK (nombre = UPPER(nombre)),
    CONSTRAINT ck_plataforma_dominio CHECK (dominio = LOWER(dominio)),
    CONSTRAINT ck_plataforma_activa CHECK (activa IN ('S', 'N'))
);

-- Respaldo de un entrenamiento terminado: una imagen (reloj inteligente, captura de una app) o el enlace a la
-- actividad en una app deportiva reconocida (PLATAFORMA_DEPORTE). Una por entrenamiento. Da un bonus de XP.
--   * Imagen: JPEG, PNG o WEBP de hasta 5 MB, guardada en la base. Opcional.
--   * Enlace: obligatorio para las sesiones de más de 180 minutos (4 horas para subirlo) y para que un
--     entrenamiento cuente en enfrentamientos y competencias. Un mismo enlace no sirve dos veces.
-- No se valida automáticamente: queda SIN_REVISAR hasta que un entrenador la mire (por ejemplo si el jugador
-- es reportado). Si la RECHAZA, el bonus se revierte con un AJUSTE.
CREATE TABLE EVIDENCIA_ENTRENAMIENTO (
    id_evidencia      NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_entrenamiento  NUMBER(10) NOT NULL,
    nombre_archivo    VARCHAR2(200),
    tipo_contenido    VARCHAR2(30),
    tamano_bytes      NUMBER(8),
    contenido         BLOB,
    url_externa       VARCHAR2(500),
    id_plataforma     NUMBER(10),
    estado_revision   VARCHAR2(12) DEFAULT 'SIN_REVISAR' NOT NULL,
    nota_revision     VARCHAR2(200),
    fecha_subida      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_revision    TIMESTAMP,
    CONSTRAINT pk_evidencia_entrenamiento PRIMARY KEY (id_evidencia),
    CONSTRAINT uq_evidencia_entrenamiento UNIQUE (id_entrenamiento),
    CONSTRAINT uq_evidencia_url UNIQUE (url_externa),
    CONSTRAINT fk_evidencia_entrenamiento FOREIGN KEY (id_entrenamiento)
        REFERENCES ENTRENAMIENTO (id_entrenamiento) ON DELETE CASCADE,
    CONSTRAINT fk_evidencia_plataforma FOREIGN KEY (id_plataforma) REFERENCES PLATAFORMA_DEPORTE (id_plataforma),
    CONSTRAINT ck_evidencia_origen CHECK (
        (contenido IS NOT NULL AND nombre_archivo IS NOT NULL AND tipo_contenido IS NOT NULL
            AND tamano_bytes IS NOT NULL AND url_externa IS NULL AND id_plataforma IS NULL)
        OR (contenido IS NULL AND nombre_archivo IS NULL AND tipo_contenido IS NULL
            AND tamano_bytes IS NULL AND url_externa IS NOT NULL AND id_plataforma IS NOT NULL)),
    CONSTRAINT ck_evidencia_tipo CHECK (tipo_contenido IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT ck_evidencia_tamano CHECK (tamano_bytes BETWEEN 1 AND 5242880),
    CONSTRAINT ck_evidencia_estado CHECK (estado_revision IN ('SIN_REVISAR', 'ACEPTADA', 'RECHAZADA')),
    CONSTRAINT ck_evidencia_revision CHECK ((estado_revision = 'SIN_REVISAR' AND fecha_revision IS NULL)
                                            OR (estado_revision <> 'SIN_REVISAR' AND fecha_revision IS NOT NULL)),
    CONSTRAINT ck_evidencia_rechazo CHECK (estado_revision <> 'RECHAZADA' OR nota_revision IS NOT NULL)
);
CREATE INDEX ix_evidencia_estado ON EVIDENCIA_ENTRENAMIENTO (estado_revision, fecha_subida);


-- =====================================================================
-- 4. COMPETITIVO: temporadas, ligas, rangos y matchmaking
-- =====================================================================

-- porcentaje_conservado: % de los puntos finales que se conservan en la temporada siguiente.
CREATE TABLE TEMPORADA (
    id_temporada           NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre                 VARCHAR2(60) NOT NULL,
    fecha_inicio           DATE NOT NULL,
    fecha_fin              DATE NOT NULL,
    porcentaje_conservado  NUMBER(3) DEFAULT 50 NOT NULL,
    estado                 VARCHAR2(10) DEFAULT 'PROGRAMADA' NOT NULL,
    CONSTRAINT pk_temporada PRIMARY KEY (id_temporada),
    CONSTRAINT uq_temporada_nombre UNIQUE (nombre),
    CONSTRAINT ck_temporada_estado CHECK (estado IN ('PROGRAMADA', 'ACTIVA', 'CERRADA')),
    CONSTRAINT ck_temporada_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_temporada_porcentaje CHECK (porcentaje_conservado BETWEEN 0 AND 100)
);
-- una sola temporada ACTIVA
CREATE UNIQUE INDEX ux_temporada_activa ON TEMPORADA (CASE WHEN estado = 'ACTIVA' THEN 1 END);

CREATE TABLE LIGA (
    id_liga         NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre          VARCHAR2(40) NOT NULL,
    orden           NUMBER(2) NOT NULL,
    cupos_ascenso   NUMBER(3) DEFAULT 0 NOT NULL,
    cupos_descenso  NUMBER(3) DEFAULT 0 NOT NULL,
    CONSTRAINT pk_liga PRIMARY KEY (id_liga),
    CONSTRAINT uq_liga_nombre UNIQUE (nombre),
    CONSTRAINT uq_liga_orden UNIQUE (orden),
    CONSTRAINT ck_liga_cupos CHECK (cupos_ascenso >= 0 AND cupos_descenso >= 0)
);

-- division NULL = la liga no tiene divisiones (por ejemplo ELITE).
CREATE TABLE RANGO (
    id_rango       NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_liga        NUMBER(10) NOT NULL,
    division       VARCHAR2(3),
    orden          NUMBER(3) NOT NULL,
    puntos_minimo  NUMBER(10) NOT NULL,
    CONSTRAINT pk_rango PRIMARY KEY (id_rango),
    CONSTRAINT uq_rango_liga_division UNIQUE (id_liga, division),
    CONSTRAINT uq_rango_orden UNIQUE (orden),
    CONSTRAINT uq_rango_puntos UNIQUE (puntos_minimo),
    CONSTRAINT fk_rango_liga FOREIGN KEY (id_liga) REFERENCES LIGA (id_liga),
    CONSTRAINT ck_rango_division CHECK (division IS NULL OR division IN ('I', 'II', 'III')),
    CONSTRAINT ck_rango_puntos CHECK (puntos_minimo >= 0)
);

CREATE TABLE PARTICIPACION_TEMPORADA (
    id_usuario         NUMBER(10) NOT NULL,
    id_temporada       NUMBER(10) NOT NULL,
    id_rango_inicial   NUMBER(10) NOT NULL,
    id_rango_actual    NUMBER(10) NOT NULL,
    puntos_iniciales   NUMBER(10) DEFAULT 0 NOT NULL,
    puntos_actuales    NUMBER(10) DEFAULT 0 NOT NULL,
    puntos_finales     NUMBER(10),
    posicion_final     NUMBER(6),
    estado             VARCHAR2(13) DEFAULT 'ACTIVA' NOT NULL,
    fecha_inscripcion  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_participacion_temporada PRIMARY KEY (id_usuario, id_temporada),
    CONSTRAINT fk_part_temp_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_part_temp_temporada FOREIGN KEY (id_temporada) REFERENCES TEMPORADA (id_temporada),
    CONSTRAINT fk_part_temp_rango_ini FOREIGN KEY (id_rango_inicial) REFERENCES RANGO (id_rango),
    CONSTRAINT fk_part_temp_rango_act FOREIGN KEY (id_rango_actual) REFERENCES RANGO (id_rango),
    -- ACTIVA: compite. FINALIZADA: la temporada cerró con el jugador en el ranking. ABANDONADA: el jugador
    -- se retiró. DESCALIFICADA: la sacó moderación (no conserva puntos para la temporada siguiente).
    CONSTRAINT ck_part_temp_estado CHECK (estado IN ('ACTIVA', 'FINALIZADA', 'ABANDONADA', 'DESCALIFICADA')),
    CONSTRAINT ck_part_temp_puntos CHECK (puntos_iniciales >= 0 AND puntos_actuales >= 0
                                          AND (puntos_finales IS NULL OR puntos_finales >= 0))
);
CREATE INDEX ix_part_temp_ranking ON PARTICIPACION_TEMPORADA (id_temporada, puntos_actuales);
CREATE INDEX ix_part_temp_rango_ini ON PARTICIPACION_TEMPORADA (id_rango_inicial);
CREATE INDEX ix_part_temp_rango_act ON PARTICIPACION_TEMPORADA (id_rango_actual);

-- Cada cambio de rango de un jugador en una temporada: el inicial (INSCRIPCION) y los que causan los
-- enfrentamientos. El backend solo puede insertar: el historial no se edita ni se borra.
CREATE TABLE HISTORIAL_RANGO (
    id_historial_rango  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario          NUMBER(10) NOT NULL,
    id_temporada        NUMBER(10) NOT NULL,
    id_rango_anterior   NUMBER(10),
    id_rango_nuevo      NUMBER(10) NOT NULL,
    puntos              NUMBER(10) NOT NULL,
    causa               VARCHAR2(15) NOT NULL,
    fecha_cambio        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_historial_rango PRIMARY KEY (id_historial_rango),
    CONSTRAINT fk_historial_participacion FOREIGN KEY (id_usuario, id_temporada)
        REFERENCES PARTICIPACION_TEMPORADA (id_usuario, id_temporada) ON DELETE CASCADE,
    CONSTRAINT fk_historial_rango_anterior FOREIGN KEY (id_rango_anterior) REFERENCES RANGO (id_rango),
    CONSTRAINT fk_historial_rango_nuevo FOREIGN KEY (id_rango_nuevo) REFERENCES RANGO (id_rango),
    CONSTRAINT ck_historial_causa CHECK (causa IN ('INSCRIPCION', 'ENFRENTAMIENTO')),
    CONSTRAINT ck_historial_cambio CHECK ((causa = 'INSCRIPCION' AND id_rango_anterior IS NULL)
                                          OR (causa <> 'INSCRIPCION' AND id_rango_anterior IS NOT NULL
                                              AND id_rango_anterior <> id_rango_nuevo)),
    CONSTRAINT ck_historial_puntos CHECK (puntos >= 0)
);
CREATE INDEX ix_historial_participacion ON HISTORIAL_RANGO (id_usuario, id_temporada, fecha_cambio);
CREATE INDEX ix_historial_rango_anterior ON HISTORIAL_RANGO (id_rango_anterior);
CREATE INDEX ix_historial_rango_nuevo ON HISTORIAL_RANGO (id_rango_nuevo);

-- Duelo de 24 horas: gana quien sume más XP de entrenamientos en la ventana.
-- COLOCACION: primer enfrentamiento del jugador en la temporada, es individual.
CREATE TABLE ENFRENTAMIENTO (
    id_enfrentamiento  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_temporada       NUMBER(10) NOT NULL,
    tipo               VARCHAR2(12) NOT NULL,
    estado             VARCHAR2(12) DEFAULT 'EN_CURSO' NOT NULL,
    fecha_inicio       TIMESTAMP NOT NULL,
    fecha_fin          TIMESTAMP NOT NULL,
    CONSTRAINT pk_enfrentamiento PRIMARY KEY (id_enfrentamiento),
    CONSTRAINT fk_enfrentamiento_temporada FOREIGN KEY (id_temporada) REFERENCES TEMPORADA (id_temporada),
    CONSTRAINT ck_enfrentamiento_tipo CHECK (tipo IN ('COLOCACION', 'ESTANDAR')),
    CONSTRAINT ck_enfrentamiento_estado CHECK (estado IN ('PROGRAMADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO')),
    CONSTRAINT ck_enfrentamiento_fechas CHECK (fecha_fin > fecha_inicio)
);
CREATE INDEX ix_enfrentamiento_temporada ON ENFRENTAMIENTO (id_temporada, estado);

CREATE TABLE PARTICIPANTE_ENFRENTAMIENTO (
    id_enfrentamiento  NUMBER(10) NOT NULL,
    id_usuario         NUMBER(10) NOT NULL,
    puntaje            NUMBER(10,2),
    resultado          VARCHAR2(12),
    CONSTRAINT pk_participante_enfrentamiento PRIMARY KEY (id_enfrentamiento, id_usuario),
    CONSTRAINT fk_participante_enfrentamiento FOREIGN KEY (id_enfrentamiento) REFERENCES ENFRENTAMIENTO (id_enfrentamiento) ON DELETE CASCADE,
    CONSTRAINT fk_participante_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_participante_resultado CHECK (resultado IS NULL OR resultado IN ('VICTORIA', 'DERROTA', 'EMPATE', 'COMPLETADO'))
);
CREATE INDEX ix_participante_usuario ON PARTICIPANTE_ENFRENTAMIENTO (id_usuario);

CREATE TABLE SOLICITUD_MATCHMAKING (
    id_solicitud           NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario             NUMBER(10) NOT NULL,
    id_temporada           NUMBER(10) NOT NULL,
    id_enfrentamiento      NUMBER(10),
    estado                 VARCHAR2(12) DEFAULT 'EN_COLA' NOT NULL,
    fecha_solicitud        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_emparejamiento   TIMESTAMP,
    CONSTRAINT pk_solicitud_matchmaking PRIMARY KEY (id_solicitud),
    CONSTRAINT fk_solicitud_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_temporada FOREIGN KEY (id_temporada) REFERENCES TEMPORADA (id_temporada),
    CONSTRAINT fk_solicitud_enfrentamiento FOREIGN KEY (id_enfrentamiento) REFERENCES ENFRENTAMIENTO (id_enfrentamiento),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('EN_COLA', 'EMPAREJADA', 'CANCELADA')),
    CONSTRAINT ck_solicitud_emparejada CHECK (
        (estado = 'EMPAREJADA' AND id_enfrentamiento IS NOT NULL AND fecha_emparejamiento IS NOT NULL)
        OR (estado <> 'EMPAREJADA' AND id_enfrentamiento IS NULL))
);
CREATE INDEX ix_solicitud_cola ON SOLICITUD_MATCHMAKING (id_temporada, estado, fecha_solicitud);
CREATE INDEX ix_solicitud_usuario ON SOLICITUD_MATCHMAKING (id_usuario);
CREATE INDEX ix_solicitud_enfrentamiento ON SOLICITUD_MATCHMAKING (id_enfrentamiento);
-- una sola solicitud EN_COLA por jugador y temporada
CREATE UNIQUE INDEX ux_solicitud_en_cola ON SOLICITUD_MATCHMAKING (
    CASE WHEN estado = 'EN_COLA' THEN id_usuario END,
    CASE WHEN estado = 'EN_COLA' THEN id_temporada END);


-- Competencias amistosas por invitación (aparte del matchmaking por ranking, que no usa invitaciones).
--   DUELO:  el creador invita a 1 jugador; empieza cuando acepta.
--   GRUPAL: 3 a 10 jugadores; empieza en fecha_inicio si aceptaron al menos 2 (creador incluido).
--   TORNEO: 4 a 16 jugadores, por rondas eliminatorias: al final de cada ronda sigue la mitad con más XP.
-- El puntaje es el XP de los entrenamientos que empezaron dentro de la ronda (de la actividad pedida, si hay).
-- fecha_fin es el fin de la ronda en curso (o de la competencia, cuando termina). No dan puntos de temporada;
-- el ganador recibe un premio de XP (MOVIMIENTO_XP con origen COMPETENCIA).
CREATE TABLE COMPETENCIA (
    id_competencia        NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_creador            NUMBER(10) NOT NULL,
    id_tipo_actividad     NUMBER(10),
    tipo                  VARCHAR2(10) NOT NULL,
    nombre                VARCHAR2(80) NOT NULL,
    estado                VARCHAR2(10) DEFAULT 'PENDIENTE' NOT NULL,
    duracion_ronda_horas  NUMBER(3) NOT NULL,
    ronda_actual          NUMBER(2) DEFAULT 0 NOT NULL,
    fecha_creacion        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_inicio          TIMESTAMP,
    fecha_fin             TIMESTAMP,
    CONSTRAINT pk_competencia PRIMARY KEY (id_competencia),
    CONSTRAINT fk_competencia_creador FOREIGN KEY (id_creador) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_competencia_actividad FOREIGN KEY (id_tipo_actividad) REFERENCES TIPO_ACTIVIDAD (id_tipo_actividad),
    CONSTRAINT ck_competencia_tipo CHECK (tipo IN ('DUELO', 'GRUPAL', 'TORNEO')),
    CONSTRAINT ck_competencia_estado CHECK (estado IN ('PENDIENTE', 'EN_CURSO', 'FINALIZADA', 'CANCELADA')),
    CONSTRAINT ck_competencia_duracion CHECK (duracion_ronda_horas BETWEEN 1 AND 168),
    -- los duelos arrancan al aceptar; las grupales y los torneos tienen fecha programada
    CONSTRAINT ck_competencia_inicio CHECK (tipo = 'DUELO' OR fecha_inicio IS NOT NULL),
    CONSTRAINT ck_competencia_en_curso CHECK (estado <> 'EN_CURSO'
                                              OR (fecha_inicio IS NOT NULL AND fecha_fin IS NOT NULL AND ronda_actual >= 1)),
    CONSTRAINT ck_competencia_rondas CHECK (tipo = 'TORNEO' OR ronda_actual <= 1),
    CONSTRAINT ck_competencia_fechas CHECK (fecha_fin IS NULL OR fecha_fin > fecha_inicio)
);
CREATE INDEX ix_competencia_estado ON COMPETENCIA (estado, fecha_fin);
CREATE INDEX ix_competencia_creador ON COMPETENCIA (id_creador);
CREATE INDEX ix_competencia_actividad ON COMPETENCIA (id_tipo_actividad);

-- estado_invitacion: INVITADO (sin responder), ACEPTADO, RECHAZADO o VENCIDA (no respondió antes del inicio).
-- El creador queda ACEPTADO. posicion y resultado se fijan al terminar.
CREATE TABLE PARTICIPANTE_COMPETENCIA (
    id_competencia      NUMBER(10) NOT NULL,
    id_usuario          NUMBER(10) NOT NULL,
    estado_invitacion   VARCHAR2(10) DEFAULT 'INVITADO' NOT NULL,
    fecha_invitacion    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_respuesta     TIMESTAMP,
    puntaje             NUMBER(10,2) DEFAULT 0 NOT NULL,
    eliminado_en_ronda  NUMBER(2),
    posicion            NUMBER(3),
    resultado           VARCHAR2(10),
    CONSTRAINT pk_participante_competencia PRIMARY KEY (id_competencia, id_usuario),
    CONSTRAINT fk_part_comp_competencia FOREIGN KEY (id_competencia)
        REFERENCES COMPETENCIA (id_competencia) ON DELETE CASCADE,
    CONSTRAINT fk_part_comp_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_part_comp_invitacion CHECK (estado_invitacion IN ('INVITADO', 'ACEPTADO', 'RECHAZADO', 'VENCIDA')),
    CONSTRAINT ck_part_comp_respuesta CHECK ((estado_invitacion = 'INVITADO' AND fecha_respuesta IS NULL)
                                             OR (estado_invitacion <> 'INVITADO' AND fecha_respuesta IS NOT NULL)),
    CONSTRAINT ck_part_comp_resultado CHECK (resultado IS NULL
                                             OR (resultado IN ('GANADOR', 'PERDEDOR', 'EMPATE')
                                                 AND estado_invitacion = 'ACEPTADO')),
    CONSTRAINT ck_part_comp_valores CHECK (puntaje >= 0 AND (posicion IS NULL OR posicion >= 1))
);
CREATE INDEX ix_part_comp_usuario ON PARTICIPANTE_COMPETENCIA (id_usuario, estado_invitacion);


-- =====================================================================
-- 5. SOCIAL Y RETOS
-- =====================================================================

CREATE TABLE CLAN (
    id_clan         NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre          VARCHAR2(60) NOT NULL,
    descripcion     VARCHAR2(300),
    capacidad_max   NUMBER(3) DEFAULT 50 NOT NULL,
    estado          VARCHAR2(10) DEFAULT 'ACTIVO' NOT NULL,
    fecha_creacion  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_clan PRIMARY KEY (id_clan),
    CONSTRAINT uq_clan_nombre UNIQUE (nombre),
    CONSTRAINT ck_clan_estado CHECK (estado IN ('ACTIVO', 'CERRADO')),
    CONSTRAINT ck_clan_capacidad CHECK (capacidad_max BETWEEN 2 AND 200)
);

-- fecha_salida NULL = membresía vigente.
CREATE TABLE MEMBRESIA_CLAN (
    id_membresia   NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_clan        NUMBER(10) NOT NULL,
    id_usuario     NUMBER(10) NOT NULL,
    rol_clan       VARCHAR2(10) DEFAULT 'MIEMBRO' NOT NULL,
    fecha_ingreso  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_salida   TIMESTAMP,
    CONSTRAINT pk_membresia_clan PRIMARY KEY (id_membresia),
    CONSTRAINT fk_membresia_clan FOREIGN KEY (id_clan) REFERENCES CLAN (id_clan),
    CONSTRAINT fk_membresia_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_membresia_rol CHECK (rol_clan IN ('LIDER', 'MIEMBRO')),
    CONSTRAINT ck_membresia_fechas CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_ingreso)
);
CREATE INDEX ix_membresia_clan ON MEMBRESIA_CLAN (id_clan, fecha_salida);
-- un jugador pertenece a un solo clan a la vez
CREATE UNIQUE INDEX ux_membresia_vigente ON MEMBRESIA_CLAN (CASE WHEN fecha_salida IS NULL THEN id_usuario END);
-- cada clan tiene un solo líder vigente
CREATE UNIQUE INDEX ux_clan_lider_vigente ON MEMBRESIA_CLAN (
    CASE WHEN fecha_salida IS NULL AND rol_clan = 'LIDER' THEN id_clan END);

-- id_tipo_actividad NULL = cualquier actividad. id_nivel_habilidad NULL = todos los niveles.
-- periodicidad TEMPORADA exige id_temporada. ambito CLAN: la meta es la suma de los aportes del clan.
CREATE TABLE RETO (
    id_reto             NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_tipo_actividad   NUMBER(10),
    id_nivel_habilidad  NUMBER(10),
    id_temporada        NUMBER(10),
    nombre              VARCHAR2(100) NOT NULL,
    descripcion         VARCHAR2(500) NOT NULL,
    condiciones         VARCHAR2(300),
    imagen_url          VARCHAR2(300),
    metrica             VARCHAR2(20) NOT NULL,
    valor_meta          NUMBER(10,2) NOT NULL,
    xp_recompensa       NUMBER(6) NOT NULL,
    periodicidad        VARCHAR2(10) NOT NULL,
    ambito              VARCHAR2(10) DEFAULT 'INDIVIDUAL' NOT NULL,
    fecha_inicio        TIMESTAMP NOT NULL,
    fecha_fin           TIMESTAMP,
    estado              VARCHAR2(10) DEFAULT 'BORRADOR' NOT NULL,
    CONSTRAINT pk_reto PRIMARY KEY (id_reto),
    CONSTRAINT fk_reto_tipo FOREIGN KEY (id_tipo_actividad) REFERENCES TIPO_ACTIVIDAD (id_tipo_actividad),
    CONSTRAINT fk_reto_nivel FOREIGN KEY (id_nivel_habilidad) REFERENCES NIVEL_HABILIDAD (id_nivel_habilidad),
    CONSTRAINT fk_reto_temporada FOREIGN KEY (id_temporada) REFERENCES TEMPORADA (id_temporada),
    -- solo lo que mide el cronómetro (MINUTOS, SESIONES) o lo que se aporta a mano (REPETICIONES)
    CONSTRAINT ck_reto_metrica CHECK (metrica IN ('REPETICIONES', 'MINUTOS', 'SESIONES')),
    CONSTRAINT ck_reto_periodicidad CHECK (periodicidad IN ('UNICA', 'DIARIA', 'SEMANAL', 'TEMPORADA')),
    CONSTRAINT ck_reto_ambito CHECK (ambito IN ('INDIVIDUAL', 'CLAN')),
    CONSTRAINT ck_reto_estado CHECK (estado IN ('BORRADOR', 'ACTIVO', 'FINALIZADO')),
    CONSTRAINT ck_reto_valores CHECK (valor_meta > 0 AND xp_recompensa > 0),
    CONSTRAINT ck_reto_fechas CHECK (fecha_fin IS NULL OR fecha_fin > fecha_inicio),
    CONSTRAINT ck_reto_temporada CHECK (periodicidad <> 'TEMPORADA' OR id_temporada IS NOT NULL)
);
CREATE INDEX ix_reto_estado ON RETO (estado);
CREATE INDEX ix_reto_tipo ON RETO (id_tipo_actividad);
CREATE INDEX ix_reto_nivel ON RETO (id_nivel_habilidad);
CREATE INDEX ix_reto_temporada ON RETO (id_temporada);

-- periodo: día de inicio del periodo del reto (DIARIA: el día, SEMANAL: el lunes, UNICA/TEMPORADA: inicio del reto).
-- progreso: suma de APORTE_RETO, guardada para leer el avance sin recalcularlo.
CREATE TABLE PARTICIPACION_RETO (
    id_participacion_reto  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_reto                NUMBER(10) NOT NULL,
    id_usuario             NUMBER(10) NOT NULL,
    id_clan                NUMBER(10),
    periodo                DATE NOT NULL,
    fecha_limite           TIMESTAMP NOT NULL,
    progreso               NUMBER(10,2) DEFAULT 0 NOT NULL,
    estado                 VARCHAR2(12) DEFAULT 'EN_CURSO' NOT NULL,
    fecha_inscripcion      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_completado       TIMESTAMP,
    CONSTRAINT pk_participacion_reto PRIMARY KEY (id_participacion_reto),
    CONSTRAINT uq_participacion_periodo UNIQUE (id_reto, id_usuario, periodo),
    CONSTRAINT fk_participacion_reto FOREIGN KEY (id_reto) REFERENCES RETO (id_reto),
    CONSTRAINT fk_participacion_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_participacion_clan FOREIGN KEY (id_clan) REFERENCES CLAN (id_clan),
    CONSTRAINT ck_participacion_estado CHECK (estado IN ('EN_CURSO', 'COMPLETADO', 'VENCIDO', 'ABANDONADO')),
    CONSTRAINT ck_participacion_progreso CHECK (progreso >= 0),
    CONSTRAINT ck_participacion_fechas CHECK (fecha_limite > fecha_inscripcion),
    CONSTRAINT ck_participacion_completado CHECK ((estado = 'COMPLETADO' AND fecha_completado IS NOT NULL)
                                                  OR (estado <> 'COMPLETADO' AND fecha_completado IS NULL))
);
CREATE INDEX ix_participacion_usuario ON PARTICIPACION_RETO (id_usuario, estado);
CREATE INDEX ix_participacion_clan ON PARTICIPACION_RETO (id_clan, id_reto, periodo);

-- id_entrenamiento NULL = aporte registrado a mano (solo retos de REPETICIONES).
CREATE TABLE APORTE_RETO (
    id_aporte              NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_participacion_reto  NUMBER(10) NOT NULL,
    id_entrenamiento       NUMBER(10),
    fecha_hora             TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    cantidad               NUMBER(10,2) NOT NULL,
    nota                   VARCHAR2(200),
    CONSTRAINT pk_aporte_reto PRIMARY KEY (id_aporte),
    CONSTRAINT fk_aporte_participacion FOREIGN KEY (id_participacion_reto) REFERENCES PARTICIPACION_RETO (id_participacion_reto) ON DELETE CASCADE,
    CONSTRAINT fk_aporte_entrenamiento FOREIGN KEY (id_entrenamiento) REFERENCES ENTRENAMIENTO (id_entrenamiento) ON DELETE CASCADE,
    CONSTRAINT ck_aporte_cantidad CHECK (cantidad > 0)
);
CREATE INDEX ix_aporte_entrenamiento ON APORTE_RETO (id_entrenamiento);
-- un entrenamiento aporta una sola vez a cada participación. (Una UNIQUE normal sobre las dos columnas
-- impediría registrar dos aportes manuales, porque Oracle compara (participación, NULL) como duplicados.)
CREATE UNIQUE INDEX ux_aporte_entrenamiento ON APORTE_RETO (
    CASE WHEN id_entrenamiento IS NOT NULL THEN id_participacion_reto END, id_entrenamiento);


-- =====================================================================
-- 6. RECOMPENSAS
-- =====================================================================

-- criterio: METRICA>=VALOR. Métricas: ENTRENAMIENTOS, MINUTOS, RACHA, XP, NIVEL, RETOS, VICTORIAS, CLAN.
CREATE TABLE LOGRO (
    id_logro     NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    nombre       VARCHAR2(80) NOT NULL,
    descripcion  VARCHAR2(300) NOT NULL,
    criterio     VARCHAR2(300) NOT NULL,
    xp_bonus     NUMBER(6) DEFAULT 0 NOT NULL,
    icono_url    VARCHAR2(300),
    CONSTRAINT pk_logro PRIMARY KEY (id_logro),
    CONSTRAINT uq_logro_nombre UNIQUE (nombre),
    CONSTRAINT ck_logro_xp CHECK (xp_bonus >= 0)
);

CREATE TABLE LOGRO_USUARIO (
    id_usuario      NUMBER(10) NOT NULL,
    id_logro        NUMBER(10) NOT NULL,
    fecha_obtenido  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_logro_usuario PRIMARY KEY (id_usuario, id_logro),
    CONSTRAINT fk_logro_usuario_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_logro_usuario_logro FOREIGN KEY (id_logro) REFERENCES LOGRO (id_logro)
);
CREATE INDEX ix_logro_usuario_logro ON LOGRO_USUARIO (id_logro);

-- Libro de XP: cada suma o resta de XP con su origen. PERFIL_JUGADOR.xp_total es la suma.
CREATE TABLE MOVIMIENTO_XP (
    id_movimiento          NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario             NUMBER(10) NOT NULL,
    id_temporada           NUMBER(10),
    id_entrenamiento       NUMBER(10),
    id_participacion_reto  NUMBER(10),
    id_logro               NUMBER(10),
    id_evidencia           NUMBER(10),
    id_competencia         NUMBER(10),
    origen                 VARCHAR2(15) NOT NULL,
    cantidad               NUMBER(8) NOT NULL,
    motivo                 VARCHAR2(200),
    fecha_movimiento       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_movimiento_xp PRIMARY KEY (id_movimiento),
    CONSTRAINT uq_movimiento_entrenamiento UNIQUE (id_entrenamiento),
    CONSTRAINT uq_movimiento_participacion UNIQUE (id_participacion_reto),
    CONSTRAINT uq_movimiento_evidencia UNIQUE (id_evidencia),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_movimiento_temporada FOREIGN KEY (id_temporada) REFERENCES TEMPORADA (id_temporada),
    CONSTRAINT fk_movimiento_entrenamiento FOREIGN KEY (id_entrenamiento) REFERENCES ENTRENAMIENTO (id_entrenamiento) ON DELETE CASCADE,
    CONSTRAINT fk_movimiento_participacion FOREIGN KEY (id_participacion_reto) REFERENCES PARTICIPACION_RETO (id_participacion_reto) ON DELETE CASCADE,
    CONSTRAINT fk_movimiento_logro FOREIGN KEY (id_logro) REFERENCES LOGRO (id_logro),
    CONSTRAINT fk_movimiento_evidencia FOREIGN KEY (id_evidencia)
        REFERENCES EVIDENCIA_ENTRENAMIENTO (id_evidencia) ON DELETE CASCADE,
    CONSTRAINT fk_movimiento_competencia FOREIGN KEY (id_competencia)
        REFERENCES COMPETENCIA (id_competencia) ON DELETE CASCADE,
    -- exactamente una referencia según el origen; AJUSTE no tiene referencia pero sí motivo y puede restar
    CONSTRAINT ck_movimiento_origen CHECK (
        (CASE WHEN id_entrenamiento IS NOT NULL THEN 1 ELSE 0 END
         + CASE WHEN id_participacion_reto IS NOT NULL THEN 1 ELSE 0 END
         + CASE WHEN id_logro IS NOT NULL THEN 1 ELSE 0 END
         + CASE WHEN id_evidencia IS NOT NULL THEN 1 ELSE 0 END
         + CASE WHEN id_competencia IS NOT NULL THEN 1 ELSE 0 END)
        = CASE WHEN origen = 'AJUSTE' THEN 0 ELSE 1 END),
    CONSTRAINT ck_movimiento_referencia CHECK (
        (origen = 'ENTRENAMIENTO' AND id_entrenamiento IS NOT NULL)
        OR (origen = 'RETO' AND id_participacion_reto IS NOT NULL)
        OR (origen = 'LOGRO' AND id_logro IS NOT NULL)
        OR (origen = 'EVIDENCIA' AND id_evidencia IS NOT NULL)
        OR (origen = 'COMPETENCIA' AND id_competencia IS NOT NULL)
        OR (origen = 'AJUSTE' AND motivo IS NOT NULL)),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad <> 0 AND (cantidad > 0 OR origen = 'AJUSTE'))
);
CREATE INDEX ix_movimiento_usuario_fecha ON MOVIMIENTO_XP (id_usuario, fecha_movimiento);
CREATE INDEX ix_movimiento_temporada ON MOVIMIENTO_XP (id_temporada);
CREATE INDEX ix_movimiento_logro ON MOVIMIENTO_XP (id_logro);
-- un logro da XP una sola vez por jugador
CREATE UNIQUE INDEX ux_movimiento_logro ON MOVIMIENTO_XP (
    CASE WHEN id_logro IS NOT NULL THEN id_usuario END, id_logro);
-- y una competencia premia una sola vez a cada jugador
CREATE UNIQUE INDEX ux_movimiento_competencia ON MOVIMIENTO_XP (
    CASE WHEN id_competencia IS NOT NULL THEN id_usuario END, id_competencia);
CREATE INDEX ix_movimiento_competencia ON MOVIMIENTO_XP (id_competencia);


-- =====================================================================
-- 7. NOTIFICACIONES
-- Se crean PENDIENTE y un proceso asíncrono las envía (ENVIADA) o registra el fallo (FALLIDA).
-- =====================================================================
CREATE TABLE NOTIFICACION (
    id_notificacion  NUMBER(10) GENERATED ALWAYS AS IDENTITY,
    id_usuario       NUMBER(10) NOT NULL,
    tipo             VARCHAR2(25) NOT NULL,
    canal            VARCHAR2(10) DEFAULT 'PUSH' NOT NULL,
    titulo           VARCHAR2(100) NOT NULL,
    mensaje          VARCHAR2(300) NOT NULL,
    estado_envio     VARCHAR2(10) DEFAULT 'PENDIENTE' NOT NULL,
    fecha_creacion   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_envio      TIMESTAMP,
    fecha_lectura    TIMESTAMP,
    CONSTRAINT pk_notificacion PRIMARY KEY (id_notificacion),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_notificacion_tipo CHECK (tipo IN ('VALIDACION', 'RACHA', 'NIVEL', 'RETO', 'LOGRO', 'CLAN', 'MATCHMAKING',
                                                    'COMPETENCIA', 'CAMBIO_RANGO', 'FIN_TEMPORADA', 'RECUPERACION',
                                                    'SEGURIDAD')),
    CONSTRAINT ck_notificacion_canal CHECK (canal IN ('PUSH', 'EMAIL')),
    CONSTRAINT ck_notificacion_estado CHECK (estado_envio IN ('PENDIENTE', 'ENVIADA', 'FALLIDA')),
    -- fecha_lectura NULL = sin leer
    CONSTRAINT ck_notificacion_lectura CHECK (fecha_lectura IS NULL OR fecha_lectura >= fecha_creacion),
    CONSTRAINT ck_notificacion_envio CHECK ((estado_envio = 'PENDIENTE' AND fecha_envio IS NULL)
                                           OR (estado_envio <> 'PENDIENTE' AND fecha_envio IS NOT NULL))
);
CREATE INDEX ix_notificacion_usuario ON NOTIFICACION (id_usuario, fecha_creacion);
CREATE INDEX ix_notificacion_estado ON NOTIFICACION (estado_envio);


-- =====================================================================
-- V. VISTAS PARA REPORTES
-- =====================================================================

-- Datos públicos de cada cuenta: sin correo, contraseña, nombres ni fecha de nacimiento.
CREATE OR REPLACE VIEW V_USUARIO_PUBLICO AS
SELECT u.id_usuario, p.gamertag, r.nombre AS rol, u.estado, u.fecha_registro,
       p.xp_total, p.racha_actual, p.racha_maxima, p.avatar_url
FROM USUARIO u
JOIN ROL r ON r.id_rol = u.id_rol
LEFT JOIN PERFIL_JUGADOR p ON p.id_usuario = u.id_usuario;

-- Ranking de cada temporada (los descalificados y los que abandonaron no aparecen).
CREATE OR REPLACE VIEW V_RANKING_TEMPORADA AS
SELECT pt.id_temporada, t.nombre AS temporada, t.estado AS estado_temporada,
       RANK() OVER (PARTITION BY pt.id_temporada ORDER BY pt.puntos_actuales DESC) AS posicion,
       pt.id_usuario, p.gamertag,
       l.nombre || CASE WHEN r.division IS NOT NULL THEN ' ' || r.division END AS rango,
       pt.puntos_iniciales, pt.puntos_actuales, pt.estado
FROM PARTICIPACION_TEMPORADA pt
JOIN TEMPORADA t ON t.id_temporada = pt.id_temporada
JOIN PERFIL_JUGADOR p ON p.id_usuario = pt.id_usuario
JOIN RANGO r ON r.id_rango = pt.id_rango_actual
JOIN LIGA l ON l.id_liga = r.id_liga
WHERE pt.estado IN ('ACTIVA', 'FINALIZADA');


-- =====================================================================
-- 8. DATOS INICIALES (catálogos necesarios para que la aplicación funcione)
-- =====================================================================

INSERT INTO ROL (nombre, descripcion) VALUES ('JUGADOR', 'Usuario que entrena, compite y participa en retos');
INSERT INTO ROL (nombre, descripcion) VALUES ('ADMINISTRADOR', 'Gestiona todo: catálogos, temporadas, retos, usuarios, soporte de cuentas, analítica y moderación');
INSERT INTO ROL (nombre, descripcion) VALUES ('ENTRENADOR', 'Modera: revisa entrenamientos pendientes y evidencias, cierra clanes y descalifica jugadores de la temporada');

-- Apps deportivas cuyos enlaces se aceptan como evidencia (la ruta es una expresión regular).
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta) VALUES ('STRAVA', 'strava.com', '^/activities/[0-9]+/?$');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta) VALUES ('STRAVA', 'strava.app.link', '^/[A-Za-z0-9]{6,20}$');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta) VALUES ('GARMIN CONNECT', 'connect.garmin.com', '^/modern/activity/[0-9]+/?$');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta) VALUES ('POLAR FLOW', 'flow.polar.com', '^/training/analysis/[0-9]+/?$');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta) VALUES ('KOMOOT', 'komoot.com', '^/tour/[0-9]+/?$');

INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('INICIACION', 1, 'Empieza o retoma el hábito de entrenar');
INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('INTERMEDIO', 2, 'Entrena con regularidad y quiere progresar');
INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('AVANZADO', 3, 'Rutina exigente y constante');
INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('ATLETA', 4, 'Entrenamiento de alto rendimiento');

-- Cuestionario: 5 preguntas, una opción por nivel. La pregunta 3 pesa doble.
DECLARE
    PROCEDURE pregunta(p_orden NUMBER, p_enunciado VARCHAR2, p_peso NUMBER,
                       o1_t VARCHAR2, o1_d VARCHAR2, o2_t VARCHAR2, o2_d VARCHAR2,
                       o3_t VARCHAR2, o3_d VARCHAR2, o4_t VARCHAR2, o4_d VARCHAR2) IS
        v_id NUMBER;
        PROCEDURE opcion(p_nivel VARCHAR2, p_titulo VARCHAR2, p_desc VARCHAR2) IS
        BEGIN
            INSERT INTO OPCION_EVALUACION (id_pregunta, id_nivel_habilidad, titulo, descripcion, puntaje)
            VALUES (v_id, (SELECT id_nivel_habilidad FROM NIVEL_HABILIDAD WHERE nombre = p_nivel), p_titulo, p_desc, p_peso);
        END;
    BEGIN
        INSERT INTO PREGUNTA_EVALUACION (enunciado, orden) VALUES (p_enunciado, p_orden) RETURNING id_pregunta INTO v_id;
        opcion('INICIACION', o1_t, o1_d);
        opcion('INTERMEDIO', o2_t, o2_d);
        opcion('AVANZADO', o3_t, o3_d);
        opcion('ATLETA', o4_t, o4_d);
    END;
BEGIN
    pregunta(1, '¿Hace cuánto entrenas de forma constante?', 1,
             'Menos de 3 meses', 'Estoy empezando o retomando el hábito',
             'Entre 3 meses y 1 año', 'Ya tengo una rutina, pero aún la consolido',
             'Entre 1 y 3 años', 'Entrenar hace parte de mi vida',
             'Más de 3 años', 'Compito o entreno con planificación');
    pregunta(2, '¿Cuántos días a la semana haces actividad física?', 1,
             '1 o 2 días', 'Entreno cuando el tiempo me lo permite',
             '3 días', 'El ejercicio hace parte de mi semana',
             '4 o 5 días', 'Tengo un plan semanal',
             '6 días o más', 'Entreno casi todos los días');
    pregunta(3, '¿Cuál es tu volumen semanal de entrenamiento de alta intensidad?', 2,
             'Ninguna sesión', 'Prefiero cargas suaves',
             '1 o 2 sesiones', 'Combino cargas moderadas y exigentes',
             '3 o 4 sesiones', 'Alta demanda metabólica',
             '5 sesiones o más', 'Optimizo picos de potencia');
    pregunta(4, '¿Cuánto dura normalmente una sesión tuya?', 1,
             'Menos de 30 minutos', 'Sesiones cortas y puntuales',
             'De 30 a 45 minutos', 'Sesiones moderadas',
             'De 45 a 90 minutos', 'Sesiones largas y completas',
             'Más de 90 minutos', 'Doble sesión o fondos largos');
    pregunta(5, '¿Cómo terminas una sesión exigente?', 1,
             'Muy agotado', 'Necesito parar antes de terminarla',
             'Cansado pero bien', 'La termino con esfuerzo',
             'Con energía', 'La completo y me recupero rápido',
             'Listo para más', 'Mi recuperación es muy rápida');
END;
/

INSERT INTO TIPO_ACTIVIDAD (nombre) VALUES ('RUNNING');
INSERT INTO TIPO_ACTIVIDAD (nombre) VALUES ('CICLISMO');
INSERT INTO TIPO_ACTIVIDAD (nombre) VALUES ('NATACION');
INSERT INTO TIPO_ACTIVIDAD (nombre) VALUES ('FUERZA');
INSERT INTO TIPO_ACTIVIDAD (nombre) VALUES ('CALISTENIA');
INSERT INTO TIPO_ACTIVIDAD (nombre) VALUES ('OTRO');

-- XP por minuto = base de la actividad x multiplicador de intensidad (BAJA 1, MEDIA 1.5, ALTA 2).
INSERT INTO REGLA_PUNTUACION (id_tipo_actividad, intensidad, xp_por_minuto, duracion_minima_min, vigente_desde)
SELECT t.id_tipo_actividad, i.intensidad,
       ROUND(CASE t.nombre WHEN 'RUNNING' THEN 2 WHEN 'CICLISMO' THEN 1.5 WHEN 'NATACION' THEN 2.5
                           WHEN 'FUERZA' THEN 2 WHEN 'CALISTENIA' THEN 2 ELSE 1 END * i.factor, 2),
       CASE t.nombre WHEN 'FUERZA' THEN 15 ELSE 10 END,
       DATE '2026-01-01'
FROM TIPO_ACTIVIDAD t
CROSS JOIN (SELECT 'BAJA' intensidad, 1 factor FROM DUAL
            UNION ALL SELECT 'MEDIA', 1.5 FROM DUAL
            UNION ALL SELECT 'ALTA', 2 FROM DUAL) i;

-- Niveles 1 a 30. Nivel 1: 0 XP, nivel 2: 100, nivel 3: 250... cada nivel pide 50 XP más que el anterior.
INSERT INTO NIVEL_PROGRESION (numero_nivel, xp_requerido)
SELECT LEVEL, 25 * (LEVEL - 1) * (LEVEL + 2) FROM DUAL CONNECT BY LEVEL <= 30;

-- Ligas y rangos: 100 puntos por rango. BRONCE III (0) ... DIAMANTE I (1400), ELITE (1500).
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('BRONCE', 1, 3, 0);
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('PLATA', 2, 3, 3);
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('ORO', 3, 3, 3);
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('PLATINO', 4, 3, 3);
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('DIAMANTE', 5, 3, 3);
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('ELITE', 6, 0, 3);

INSERT INTO RANGO (id_liga, division, orden, puntos_minimo)
SELECT l.id_liga, d.division, (l.orden - 1) * 3 + d.paso, ((l.orden - 1) * 3 + d.paso - 1) * 100
FROM LIGA l
CROSS JOIN (SELECT 'III' division, 1 paso FROM DUAL
            UNION ALL SELECT 'II', 2 FROM DUAL
            UNION ALL SELECT 'I', 3 FROM DUAL) d
WHERE l.nombre <> 'ELITE';
INSERT INTO RANGO (id_liga, division, orden, puntos_minimo)
VALUES ((SELECT id_liga FROM LIGA WHERE nombre = 'ELITE'), NULL, 16, 1500);

-- Temporadas: la anterior cerrada y la actual activa.
INSERT INTO TEMPORADA (nombre, fecha_inicio, fecha_fin, porcentaje_conservado, estado)
VALUES ('Temporada 0', TRUNC(SYSDATE) - 110, TRUNC(SYSDATE) - 21, 50, 'CERRADA');
INSERT INTO TEMPORADA (nombre, fecha_inicio, fecha_fin, porcentaje_conservado, estado)
VALUES ('Temporada 1', TRUNC(SYSDATE) - 20, TRUNC(SYSDATE) + 70, 50, 'ACTIVA');

INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Primeros pasos', 'Registra tu primer entrenamiento validado.', 'ENTRENAMIENTOS>=1', 20);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Racha de 3', 'Entrena 3 días seguidos.', 'RACHA>=3', 30);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Constancia', 'Suma 10 entrenamientos validados.', 'ENTRENAMIENTOS>=10', 50);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Primera victoria', 'Gana tu primer enfrentamiento.', 'VICTORIAS>=1', 50);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Espíritu de equipo', 'Únete a un clan.', 'CLAN>=1', 25);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Retador', 'Completa 3 retos.', 'RETOS>=3', 60);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Mil minutos', 'Acumula 1000 minutos de entrenamiento validado.', 'MINUTOS>=1000', 80);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES
    ('Nivel 10', 'Alcanza el nivel 10.', 'NIVEL>=10', 100);

-- Retos (fechas relativas para que estén vigentes al ejecutar el script)
DECLARE
    PROCEDURE reto(p_nombre VARCHAR2, p_desc VARCHAR2, p_metrica VARCHAR2, p_meta NUMBER, p_xp NUMBER,
                   p_periodicidad VARCHAR2, p_actividad VARCHAR2 DEFAULT NULL, p_nivel VARCHAR2 DEFAULT NULL,
                   p_ambito VARCHAR2 DEFAULT 'INDIVIDUAL', p_estado VARCHAR2 DEFAULT 'ACTIVO',
                   p_condiciones VARCHAR2 DEFAULT NULL) IS
    BEGIN
        INSERT INTO RETO (id_tipo_actividad, id_nivel_habilidad, id_temporada, nombre, descripcion, condiciones,
                          metrica, valor_meta, xp_recompensa, periodicidad, ambito, fecha_inicio, fecha_fin, estado)
        VALUES ((SELECT id_tipo_actividad FROM TIPO_ACTIVIDAD WHERE nombre = p_actividad),
                (SELECT id_nivel_habilidad FROM NIVEL_HABILIDAD WHERE nombre = p_nivel),
                CASE WHEN p_periodicidad = 'TEMPORADA' THEN (SELECT id_temporada FROM TEMPORADA WHERE estado = 'ACTIVA') END,
                p_nombre, p_desc, p_condiciones, p_metrica, p_meta, p_xp, p_periodicidad, p_ambito,
                CAST(TRUNC(SYSDATE) - 30 AS TIMESTAMP),
                CASE WHEN p_periodicidad = 'UNICA' THEN CAST(TRUNC(SYSDATE) + 60 AS TIMESTAMP) END,
                p_estado);
    END;
BEGIN
    reto('Primer paso', 'Registra tu primer entrenamiento validado.', 'SESIONES', 1, 30, 'UNICA');
    reto('Misión diaria', 'Entrena 30 minutos hoy.', 'MINUTOS', 30, 20, 'DIARIA');
    reto('Corredor de la semana', 'Suma 90 minutos de running esta semana.', 'MINUTOS', 90, 80, 'SEMANAL', 'RUNNING');
    reto('Arranque', 'Suma 60 minutos de entrenamiento.', 'MINUTOS', 60, 50, 'UNICA', NULL, 'INICIACION');
    reto('150 minutos activos', 'Alcanza los 150 minutos semanales que recomienda la OMS.', 'MINUTOS', 150, 90,
         'SEMANAL', NULL, 'INTERMEDIO');
    reto('Modo élite', 'Suma 600 minutos de entrenamiento.', 'MINUTOS', 600, 160, 'UNICA', NULL, 'AVANZADO');
    reto('Cien flexiones', 'Registra 100 flexiones en el día.', 'REPETICIONES', 100, 40, 'DIARIA', 'CALISTENIA', NULL,
         'INDIVIDUAL', 'ACTIVO', 'Registra cada serie como aporte manual.');
    reto('Constancia de temporada', 'Completa 10 sesiones durante la temporada.', 'SESIONES', 10, 120, 'TEMPORADA');
    reto('Minutos en equipo', 'Entre todo el clan sumen 360 minutos de entrenamiento.', 'MINUTOS', 360, 150, 'UNICA',
         NULL, NULL, 'CLAN');
    reto('Veinte sesiones', 'Completa 20 sesiones de entrenamiento.', 'SESIONES', 20, 100, 'UNICA', NULL, NULL,
         'INDIVIDUAL', 'BORRADOR');
END;
/

-- Administrador de la plataforma (contraseña: Admin2026). Cambiarla después de la entrega.
INSERT INTO USUARIO (id_rol, nombres, apellidos, correo, password_hash, acepta_politica_datos, fecha_acepta_politica)
VALUES ((SELECT id_rol FROM ROL WHERE nombre = 'ADMINISTRADOR'), 'Administrador', 'Kinet', 'admin@kinet.com',
        '$2a$10$mpAUEkHYLGy2Ne9fRYy.BObxcaWrcUXAYTtwWrRHyphhvVio/ZSz.', 'S', SYSDATE);
INSERT INTO PERFIL_JUGADOR (id_usuario, gamertag)
VALUES ((SELECT id_usuario FROM USUARIO WHERE correo = 'admin@kinet.com'), 'admin_kinet');

COMMIT;


-- =====================================================================
-- 9. DATOS DE PRUEBA (solo desarrollo)
-- Fechas relativas a SYSDATE. El XP de cada entrenamiento se calcula con REGLA_PUNTUACION y
-- PERFIL_JUGADOR.xp_total se recalcula al final como la suma de MOVIMIENTO_XP, así que siempre cuadran.
--
-- Cuentas para entrar (una por rol):
--   laura@kinet.com      JUGADOR       INTERMEDIO, nivel 6, racha 3, en el clan, con evidencia, retos en curso,
--                                      un duelo ganado, una competencia grupal organizada y una invitación
--                                      pendiente a un torneo
--   entrenador@kinet.com ENTRENADOR    revisa la sesión pendiente de Carlos y las evidencias
--   admin@kinet.com      ADMINISTRADOR todo lo del equipo (contraseña Admin2026)
-- Más cuentas (todas Kinet2026): marta@, diego@, valentina@, andres@, camila@ (jugadores activos con clanes, ranking,
--   entrenamientos de cada actividad y competencias), juan@ (BLOQUEADO), lucia@ (INACTIVO) y entrenador2@ (ENTRENADOR).
-- Rivales de apoyo (para el clan, el ranking, los enfrentamientos y las competencias de Laura):
--   carlos@kinet.com     JUGADOR       AVANZADO, líder del clan, 1 sesión PENDIENTE con enlace de Strava
--   sofia@kinet.com      JUGADOR       INICIACION, colocación en curso, 1 rechazado, invitaciones pendientes
-- Todos con contraseña: Kinet2026 (salvo el administrador)
-- =====================================================================
-- Funciones auxiliares temporales (se borran al final de esta sección)
CREATE OR REPLACE FUNCTION tmp_ts(p_dias NUMBER, p_horas NUMBER) RETURN TIMESTAMP IS
BEGIN
    RETURN CAST(TRUNC(SYSDATE) + p_dias + p_horas / 24 AS TIMESTAMP);
END;
/
CREATE OR REPLACE FUNCTION tmp_id(p_tabla VARCHAR2, p_nombre VARCHAR2) RETURN NUMBER IS
    v NUMBER;
BEGIN
    EXECUTE IMMEDIATE 'SELECT ' || CASE p_tabla WHEN 'ROL' THEN 'id_rol' WHEN 'TIPO_ACTIVIDAD' THEN 'id_tipo_actividad'
                      WHEN 'NIVEL_HABILIDAD' THEN 'id_nivel_habilidad' WHEN 'RETO' THEN 'id_reto'
                      WHEN 'LOGRO' THEN 'id_logro'
                      WHEN 'TEMPORADA' THEN 'id_temporada' END
                      || ' FROM ' || p_tabla || ' WHERE nombre = :n' INTO v USING p_nombre;
    RETURN v;
END;
/
CREATE OR REPLACE FUNCTION tmp_rango(p_puntos NUMBER) RETURN NUMBER IS
    v NUMBER;
BEGIN
    SELECT id_rango INTO v FROM RANGO
    WHERE puntos_minimo = (SELECT MAX(puntos_minimo) FROM RANGO WHERE puntos_minimo <= p_puntos);
    RETURN v;
END;
/
CREATE OR REPLACE FUNCTION tmp_temporada(p_fecha TIMESTAMP) RETURN NUMBER IS
    v NUMBER;
BEGIN
    SELECT MAX(id_temporada) INTO v FROM TEMPORADA
    WHERE CAST(p_fecha AS DATE) >= fecha_inicio AND CAST(p_fecha AS DATE) < fecha_fin + 1;
    RETURN v;
END;
/

DECLARE
    c_hash CONSTANT VARCHAR2(100) := '$2a$10$ZNJT9ADLFrUSdbzGmIw7Zu6XhBUlFXI4V.FGXz1qRyUNswrNKI3NS';
    c_png  CONSTANT VARCHAR2(200) := '89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000D49444154'
                                     || '78DA636460F85F0F0002870180EB47BA920000000049454E44AE426082';
    v_t0 NUMBER; v_t1 NUMBER;
    v_laura NUMBER; v_carlos NUMBER; v_sofia NUMBER; v_otro NUMBER;
    v_clan NUMBER; v_evidencia NUMBER;
    l1 NUMBER; l2 NUMBER; l3 NUMBER; l4 NUMBER; l6 NUMBER;
    c1 NUMBER; c2 NUMBER; c3 NUMBER; c4 NUMBER; c5 NUMBER;
    s1 NUMBER;
    v_part NUMBER; v_enf NUMBER; v_comp NUMBER;
    -- datos de prueba adicionales
    v_marta NUMBER; v_diego NUMBER; v_vale NUMBER; v_andres NUMBER; v_cami NUMBER; v_juan NUMBER; v_lucia NUMBER;
    v_ent2 NUMBER; v_clan2 NUMBER; v_clan3 NUMBER;
    m1 NUMBER; m2 NUMBER; m3 NUMBER; m4 NUMBER; m5 NUMBER; m6 NUMBER;
    d1 NUMBER; d2 NUMBER; d3 NUMBER; d4 NUMBER; d5 NUMBER; d6 NUMBER; d7 NUMBER;
    a1 NUMBER; a2 NUMBER; a3 NUMBER; a4 NUMBER; a5 NUMBER;
    w1 NUMBER; w2 NUMBER; w3 NUMBER; w4 NUMBER; w5 NUMBER;
    k1 NUMBER; k2 NUMBER; z1 NUMBER; z2 NUMBER;

    FUNCTION f_usuario(p_rol VARCHAR2, p_nombres VARCHAR2, p_apellidos VARCHAR2, p_correo VARCHAR2, p_gamertag VARCHAR2,
                       p_estado VARCHAR2, p_dias_registro NUMBER, p_nacimiento DATE) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO USUARIO (id_rol, nombres, apellidos, correo, password_hash, estado, acepta_politica_datos,
                             fecha_acepta_politica, fecha_registro)
        VALUES (tmp_id('ROL', p_rol), p_nombres, p_apellidos, p_correo, c_hash, p_estado, 'S',
                TRUNC(SYSDATE) - p_dias_registro, tmp_ts(-p_dias_registro, 0))
        RETURNING id_usuario INTO v;
        INSERT INTO PERFIL_JUGADOR (id_usuario, gamertag, fecha_nacimiento) VALUES (v, p_gamertag, p_nacimiento);
        RETURN v;
    END;

    PROCEDURE p_evaluacion(p_usuario NUMBER, p_dias NUMBER, n1 VARCHAR2, n2 VARCHAR2, n3 VARCHAR2, n4 VARCHAR2,
                           n5 VARCHAR2, p_resultado VARCHAR2) IS
        v NUMBER;
        TYPE t_niveles IS VARRAY(5) OF VARCHAR2(20);
        v_niveles t_niveles := t_niveles(n1, n2, n3, n4, n5);
    BEGIN
        INSERT INTO EVALUACION_USUARIO (id_usuario, id_nivel_habilidad, version_algoritmo, fecha_evaluacion)
        VALUES (p_usuario, tmp_id('NIVEL_HABILIDAD', p_resultado), 'VOTO_PONDERADO_V1', tmp_ts(-p_dias, 0.5))
        RETURNING id_evaluacion INTO v;
        FOR i IN 1 .. 5 LOOP
            INSERT INTO RESPUESTA_EVALUACION (id_evaluacion, id_opcion)
            SELECT v, o.id_opcion FROM OPCION_EVALUACION o JOIN PREGUNTA_EVALUACION p ON p.id_pregunta = o.id_pregunta
            WHERE p.orden = i AND o.id_nivel_habilidad = tmp_id('NIVEL_HABILIDAD', v_niveles(i));
        END LOOP;
    END;

    -- Inserta el entrenamiento y, si quedó validado, su movimiento de XP según la regla vigente.
    FUNCTION f_entreno(p_usuario NUMBER, p_actividad VARCHAR2, p_inicio TIMESTAMP, p_min NUMBER, p_intensidad VARCHAR2,
                       p_estado VARCHAR2 DEFAULT 'VALIDADO', p_motivo VARCHAR2 DEFAULT NULL, p_km NUMBER DEFAULT NULL,
                       p_kcal NUMBER DEFAULT NULL,
                       p_notas VARCHAR2 DEFAULT NULL) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO ENTRENAMIENTO (id_usuario, id_tipo_actividad, fecha_hora_inicio,
                                   fecha_hora_fin, duracion_min, intensidad, distancia_km, calorias_kcal, notas,
                                   estado_validacion, motivo_rechazo, fecha_registro)
        VALUES (p_usuario, tmp_id('TIPO_ACTIVIDAD', p_actividad), p_inicio,
                p_inicio + NUMTODSINTERVAL(p_min, 'MINUTE'), p_min, p_intensidad, p_km, p_kcal, p_notas,
                p_estado, p_motivo, p_inicio + NUMTODSINTERVAL(p_min + 15, 'MINUTE'))
        RETURNING id_entrenamiento INTO v;
        IF p_estado = 'VALIDADO' THEN
            INSERT INTO MOVIMIENTO_XP (id_usuario, id_temporada, id_entrenamiento, origen, cantidad, fecha_movimiento)
            SELECT p_usuario, tmp_temporada(p_inicio), v, 'ENTRENAMIENTO', ROUND(p_min * r.xp_por_minuto),
                   p_inicio + NUMTODSINTERVAL(p_min + 15, 'MINUTE')
            FROM REGLA_PUNTUACION r
            WHERE r.id_tipo_actividad = tmp_id('TIPO_ACTIVIDAD', p_actividad) AND r.intensidad = p_intensidad
              AND r.vigente_hasta IS NULL;
        END IF;
        RETURN v;
    END;

    FUNCTION f_participar(p_usuario NUMBER, p_reto VARCHAR2, p_inscripcion TIMESTAMP, p_progreso NUMBER,
                          p_estado VARCHAR2, p_completado TIMESTAMP DEFAULT NULL, p_clan NUMBER DEFAULT NULL) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        INSERT INTO PARTICIPACION_RETO (id_reto, id_usuario, id_clan, periodo, fecha_limite, progreso, estado,
                                        fecha_inscripcion, fecha_completado)
        SELECT r.id_reto, p_usuario, p_clan,
               CASE WHEN r.periodicidad = 'TEMPORADA' THEN t.fecha_inicio ELSE TRUNC(CAST(r.fecha_inicio AS DATE)) END,
               NVL(r.fecha_fin, CAST(t.fecha_fin + 1 AS TIMESTAMP)), p_progreso, p_estado, p_inscripcion, p_completado
        FROM RETO r LEFT JOIN TEMPORADA t ON t.id_temporada = r.id_temporada
        WHERE r.nombre = p_reto;
        SELECT MAX(id_participacion_reto) INTO v FROM PARTICIPACION_RETO
        WHERE id_usuario = p_usuario AND id_reto = tmp_id('RETO', p_reto);
        IF p_estado = 'COMPLETADO' THEN
            INSERT INTO MOVIMIENTO_XP (id_usuario, id_temporada, id_participacion_reto, origen, cantidad, fecha_movimiento)
            SELECT p_usuario, tmp_temporada(p_completado), v, 'RETO', r.xp_recompensa, p_completado
            FROM RETO r WHERE r.nombre = p_reto;
        END IF;
        RETURN v;
    END;

    PROCEDURE p_aporte(p_part NUMBER, p_entreno NUMBER, p_cantidad NUMBER) IS
    BEGIN
        INSERT INTO APORTE_RETO (id_participacion_reto, id_entrenamiento, fecha_hora, cantidad)
        SELECT p_part, p_entreno, fecha_registro, p_cantidad FROM ENTRENAMIENTO WHERE id_entrenamiento = p_entreno;
    END;

    PROCEDURE p_logro(p_usuario NUMBER, p_logro VARCHAR2, p_fecha TIMESTAMP) IS
    BEGIN
        INSERT INTO LOGRO_USUARIO (id_usuario, id_logro, fecha_obtenido) VALUES (p_usuario, tmp_id('LOGRO', p_logro), p_fecha);
        INSERT INTO MOVIMIENTO_XP (id_usuario, id_temporada, id_logro, origen, cantidad, fecha_movimiento)
        SELECT p_usuario, tmp_temporada(p_fecha), id_logro, 'LOGRO', xp_bonus, p_fecha FROM LOGRO WHERE nombre = p_logro;
    END;

    -- p_leida 'S': se leyó una hora después de crearse
    PROCEDURE p_aviso(p_usuario NUMBER, p_tipo VARCHAR2, p_titulo VARCHAR2, p_mensaje VARCHAR2, p_fecha TIMESTAMP,
                      p_leida VARCHAR2 DEFAULT 'S', p_canal VARCHAR2 DEFAULT 'PUSH', p_estado VARCHAR2 DEFAULT 'ENVIADA') IS
    BEGIN
        INSERT INTO NOTIFICACION (id_usuario, tipo, canal, titulo, mensaje, estado_envio, fecha_creacion, fecha_envio,
                                  fecha_lectura)
        VALUES (p_usuario, p_tipo, p_canal, p_titulo, p_mensaje, p_estado, p_fecha,
                CASE WHEN p_estado = 'PENDIENTE' THEN NULL ELSE p_fecha + INTERVAL '2' SECOND END,
                CASE WHEN p_leida = 'S' THEN p_fecha + INTERVAL '1' HOUR END);
    END;

    -- Inscripción en una temporada con su primera fila de HISTORIAL_RANGO.
    PROCEDURE p_participacion_temporada(p_usuario NUMBER, p_temporada NUMBER, p_ini NUMBER, p_act NUMBER,
                                        p_inscripcion TIMESTAMP, p_finales NUMBER DEFAULT NULL,
                                        p_posicion NUMBER DEFAULT NULL, p_estado VARCHAR2 DEFAULT 'ACTIVA') IS
    BEGIN
        INSERT INTO PARTICIPACION_TEMPORADA (id_usuario, id_temporada, id_rango_inicial, id_rango_actual,
                                             puntos_iniciales, puntos_actuales, puntos_finales, posicion_final,
                                             estado, fecha_inscripcion)
        VALUES (p_usuario, p_temporada, tmp_rango(p_ini), tmp_rango(p_act), p_ini, p_act, p_finales, p_posicion,
                p_estado, p_inscripcion);
        INSERT INTO HISTORIAL_RANGO (id_usuario, id_temporada, id_rango_anterior, id_rango_nuevo, puntos, causa, fecha_cambio)
        VALUES (p_usuario, p_temporada, NULL, tmp_rango(p_ini), p_ini, 'INSCRIPCION', p_inscripcion);
    END;

    PROCEDURE p_cambio_rango(p_usuario NUMBER, p_temporada NUMBER, p_antes NUMBER, p_despues NUMBER, p_fecha TIMESTAMP) IS
    BEGIN
        INSERT INTO HISTORIAL_RANGO (id_usuario, id_temporada, id_rango_anterior, id_rango_nuevo, puntos, causa, fecha_cambio)
        VALUES (p_usuario, p_temporada, tmp_rango(p_antes), tmp_rango(p_despues), p_despues, 'ENFRENTAMIENTO', p_fecha);
    END;

    PROCEDURE p_invitado(p_comp NUMBER, p_usuario NUMBER, p_estado VARCHAR2, p_invitacion TIMESTAMP,
                         p_respuesta TIMESTAMP DEFAULT NULL, p_puntaje NUMBER DEFAULT 0, p_posicion NUMBER DEFAULT NULL,
                         p_resultado VARCHAR2 DEFAULT NULL) IS
    BEGIN
        INSERT INTO PARTICIPANTE_COMPETENCIA (id_competencia, id_usuario, estado_invitacion, fecha_invitacion,
                                              fecha_respuesta, puntaje, posicion, resultado)
        VALUES (p_comp, p_usuario, p_estado, p_invitacion, p_respuesta, p_puntaje, p_posicion, p_resultado);
    END;

    -- Enfrentamiento con sus participantes y las solicitudes que lo originaron.
    PROCEDURE p_enfrentamiento(p_tipo VARCHAR2, p_estado VARCHAR2, p_inicio TIMESTAMP,
                               u1 NUMBER, pj1 NUMBER, r1 VARCHAR2, u2 NUMBER DEFAULT NULL, pj2 NUMBER DEFAULT NULL,
                               r2 VARCHAR2 DEFAULT NULL) IS
        v NUMBER;
    BEGIN
        INSERT INTO ENFRENTAMIENTO (id_temporada, tipo, estado, fecha_inicio, fecha_fin)
        VALUES (v_t1, p_tipo, p_estado, p_inicio, p_inicio + INTERVAL '24' HOUR)
        RETURNING id_enfrentamiento INTO v;
        INSERT INTO PARTICIPANTE_ENFRENTAMIENTO (id_enfrentamiento, id_usuario, puntaje, resultado) VALUES (v, u1, pj1, r1);
        INSERT INTO SOLICITUD_MATCHMAKING (id_usuario, id_temporada, id_enfrentamiento, estado, fecha_solicitud,
                                           fecha_emparejamiento)
        VALUES (u1, v_t1, v, 'EMPAREJADA', p_inicio - INTERVAL '1' HOUR, p_inicio);
        IF u2 IS NOT NULL THEN
            INSERT INTO PARTICIPANTE_ENFRENTAMIENTO (id_enfrentamiento, id_usuario, puntaje, resultado) VALUES (v, u2, pj2, r2);
            INSERT INTO SOLICITUD_MATCHMAKING (id_usuario, id_temporada, id_enfrentamiento, estado, fecha_solicitud,
                                               fecha_emparejamiento)
            VALUES (u2, v_t1, v, 'EMPAREJADA', p_inicio, p_inicio);
        END IF;
    END;
BEGIN
    v_t0 := tmp_id('TEMPORADA', 'Temporada 0');
    v_t1 := tmp_id('TEMPORADA', 'Temporada 1');

    -- ---------- Usuarios ----------
    v_laura  := f_usuario('JUGADOR', 'Laura', 'Gómez', 'laura@kinet.com', 'laura_runner', 'ACTIVO', 25, DATE '2001-04-15');
    v_carlos := f_usuario('JUGADOR', 'Carlos', 'Ramírez', 'carlos@kinet.com', 'carlos_pro', 'ACTIVO', 120, DATE '1998-09-02');
    v_sofia  := f_usuario('JUGADOR', 'Sofía', 'Martínez', 'sofia@kinet.com', 'sofi_fit', 'ACTIVO', 3, DATE '2004-01-20');
    v_otro   := f_usuario('ENTRENADOR', 'Mónica', 'Rivas', 'entrenador@kinet.com', 'entrenador_monica', 'ACTIVO', 60, NULL);

    -- ---------- Evaluaciones (una opción por pregunta; la pregunta 3 pesa doble) ----------
    p_evaluacion(v_laura, 25, 'INTERMEDIO', 'INTERMEDIO', 'INTERMEDIO', 'AVANZADO', 'INICIACION', 'INTERMEDIO');
    p_evaluacion(v_carlos, 120, 'AVANZADO', 'ATLETA', 'AVANZADO', 'AVANZADO', 'AVANZADO', 'AVANZADO');
    p_evaluacion(v_sofia, 3, 'INICIACION', 'INICIACION', 'INICIACION', 'INTERMEDIO', 'INICIACION', 'INICIACION');

    -- ---------- Entrenamientos ----------
    -- Laura: 120 + 135 + 180 + 180 + 90 = 705 XP; racha de 3 días (-3, -2, -1)
    l1 := f_entreno(v_laura, 'RUNNING', tmp_ts(-12, 7), 40, 'MEDIA', p_km => 6.5, p_kcal => 380);
    l6 := f_entreno(v_laura, 'CICLISMO', tmp_ts(-6, 19), 60, 'MEDIA', p_km => 20, p_kcal => 450);
    l2 := f_entreno(v_laura, 'RUNNING', tmp_ts(-3, 7), 45, 'ALTA', p_km => 8, p_kcal => 520);
    l3 := f_entreno(v_laura, 'FUERZA', tmp_ts(-2, 18), 60, 'MEDIA', p_kcal => 300, p_notas => 'Pierna y core');
    l4 := f_entreno(v_laura, 'RUNNING', tmp_ts(-1, 7), 30, 'MEDIA', p_km => 5, p_kcal => 280);
    v_part := f_entreno(v_laura, 'OTRO', tmp_ts(-1, 20), 5, 'BAJA', 'RECHAZADO',
                        'La sesión debe durar al menos 10 minutos para contar.');

    -- Carlos: 240 + 360 + 169 + 240 = 1009 XP; racha de 2 (-2, -1); una sesión larga pendiente de moderación
    c1 := f_entreno(v_carlos, 'RUNNING', tmp_ts(-15, 6), 60, 'ALTA', p_km => 12, p_kcal => 750);
    c2 := f_entreno(v_carlos, 'CICLISMO', tmp_ts(-6, 6), 120, 'ALTA', p_km => 45, p_kcal => 1400);
    c3 := f_entreno(v_carlos, 'NATACION', tmp_ts(-2, 6), 45, 'MEDIA', p_kcal => 500);
    c4 := f_entreno(v_carlos, 'RUNNING', tmp_ts(-1, 6), 60, 'ALTA', p_km => 13, p_kcal => 800);
    c5 := f_entreno(v_carlos, 'FUERZA', tmp_ts(-1, 17), 200, 'MEDIA', 'PENDIENTE',
                    p_notas => 'Sesión larga de preparación');

    -- Sofía: 40 XP y uno rechazado
    s1 := f_entreno(v_sofia, 'CALISTENIA', tmp_ts(-1, 19), 20, 'BAJA', p_kcal => 120);
    v_part := f_entreno(v_sofia, 'OTRO', tmp_ts(-1, 21), 5, 'MEDIA', 'RECHAZADO',
                        'La sesión debe durar al menos 10 minutos para contar.');

    -- ---------- Clan ----------
    INSERT INTO CLAN (nombre, descripcion, capacidad_max, fecha_creacion)
    VALUES ('Los Imparables', 'Clan de corredores y ciclistas del Quindío', 20, tmp_ts(-17, 12))
    RETURNING id_clan INTO v_clan;
    INSERT INTO MEMBRESIA_CLAN (id_clan, id_usuario, rol_clan, fecha_ingreso) VALUES (v_clan, v_carlos, 'LIDER', tmp_ts(-17, 12));
    INSERT INTO MEMBRESIA_CLAN (id_clan, id_usuario, rol_clan, fecha_ingreso) VALUES (v_clan, v_laura, 'MIEMBRO', tmp_ts(-14, 18));

    -- ---------- Retos ----------
    -- Primer paso: completado por los tres (+30 cada uno)
    v_part := f_participar(v_carlos, 'Primer paso', tmp_ts(-16, 8), 1, 'COMPLETADO', tmp_ts(-15, 7.25));
    p_aporte(v_part, c1, 1);
    v_part := f_participar(v_laura, 'Primer paso', tmp_ts(-13, 8), 1, 'COMPLETADO', tmp_ts(-12, 7.9));
    p_aporte(v_part, l1, 1);
    v_part := f_participar(v_sofia, 'Primer paso', tmp_ts(-2, 10), 1, 'COMPLETADO', tmp_ts(-1, 19.6));
    p_aporte(v_part, s1, 1);

    -- Minutos en equipo (CLAN, meta 360): Carlos 60 + 120 + 60 y Laura 40 + 60 + 45 = 385 minutos.
    -- Antes del último entrenamiento de Carlos sumaban 325; con él se completa para los dos.
    v_part := f_participar(v_carlos, 'Minutos en equipo', tmp_ts(-16, 9), 240, 'COMPLETADO', tmp_ts(-1, 7.25), v_clan);
    p_aporte(v_part, c1, 60); p_aporte(v_part, c2, 120); p_aporte(v_part, c4, 60);
    v_part := f_participar(v_laura, 'Minutos en equipo', tmp_ts(-13, 9), 145, 'COMPLETADO', tmp_ts(-1, 7.25), v_clan);
    p_aporte(v_part, l1, 40); p_aporte(v_part, l6, 60); p_aporte(v_part, l2, 45);

    -- En curso
    v_part := f_participar(v_laura, 'Constancia de temporada', tmp_ts(-15, 8), 5, 'EN_CURSO');
    p_aporte(v_part, l1, 1); p_aporte(v_part, l6, 1); p_aporte(v_part, l2, 1); p_aporte(v_part, l3, 1); p_aporte(v_part, l4, 1);
    v_part := f_participar(v_carlos, 'Modo élite', tmp_ts(-16, 10), 285, 'EN_CURSO');
    p_aporte(v_part, c1, 60); p_aporte(v_part, c2, 120); p_aporte(v_part, c3, 45); p_aporte(v_part, c4, 60);
    v_part := f_participar(v_sofia, 'Arranque', tmp_ts(-2, 10), 20, 'EN_CURSO');
    p_aporte(v_part, s1, 20);

    -- ---------- Logros ----------
    p_logro(v_carlos, 'Espíritu de equipo', tmp_ts(-17, 12));
    p_logro(v_carlos, 'Primeros pasos', tmp_ts(-15, 7.26));
    p_logro(v_carlos, 'Primera victoria', tmp_ts(-5, 5));
    p_logro(v_laura, 'Espíritu de equipo', tmp_ts(-14, 18));
    p_logro(v_laura, 'Primeros pasos', tmp_ts(-12, 7.91));
    p_logro(v_laura, 'Racha de 3', tmp_ts(-1, 7.76));
    p_logro(v_sofia, 'Primeros pasos', tmp_ts(-1, 19.61));

    -- Evidencias. Laura sube una imagen PNG de 1x1 como marcador y recibe el bonus de 10 XP; Carlos sube el
    -- enlace de Strava que exige su sesión larga, que espera la revisión del entrenador y todavía no da XP.
    INSERT INTO EVIDENCIA_ENTRENAMIENTO (id_entrenamiento, nombre_archivo, tipo_contenido, tamano_bytes, contenido, fecha_subida)
    VALUES (l2, 'captura-reloj.png', 'image/png', 70, TO_BLOB(HEXTORAW(c_png)), tmp_ts(-3, 8.5))
    RETURNING id_evidencia INTO v_evidencia;
    INSERT INTO MOVIMIENTO_XP (id_usuario, id_temporada, id_evidencia, origen, cantidad, fecha_movimiento)
    VALUES (v_laura, v_t1, v_evidencia, 'EVIDENCIA', 10, tmp_ts(-3, 8.5));
    INSERT INTO EVIDENCIA_ENTRENAMIENTO (id_entrenamiento, url_externa, id_plataforma, fecha_subida)
    VALUES (c5, 'https://www.strava.com/activities/10234567890',
            (SELECT id_plataforma FROM PLATAFORMA_DEPORTE WHERE dominio = 'strava.com'), tmp_ts(-1, 21));

    -- Ajuste manual hecho por el administrador
    INSERT INTO MOVIMIENTO_XP (id_usuario, id_temporada, origen, cantidad, motivo, fecha_movimiento)
    VALUES (v_sofia, v_t1, 'AJUSTE', 10, 'Compensación por falla de sincronización', tmp_ts(-1, 22));

    -- ---------- Temporadas y matchmaking ----------
    -- Temporada 0 (cerrada): Carlos terminó primero con 400 puntos (subió de BRONCE III a PLATA II)
    p_participacion_temporada(v_carlos, v_t0, 0, 400, tmp_ts(-100, 9), 400, 1, 'FINALIZADA');
    p_cambio_rango(v_carlos, v_t0, 0, 400, tmp_ts(-60, 9));
    -- Temporada 1: Carlos conserva el 50 % (200, BRONCE I); colocación +80 y victoria +25 = 305 (PLATA III)
    p_participacion_temporada(v_carlos, v_t1, 200, 305, tmp_ts(-15, 5));
    p_cambio_rango(v_carlos, v_t1, 280, 305, tmp_ts(-5, 5));
    -- Laura: colocación +40 y derrota -15 = 25 (BRONCE III)
    p_participacion_temporada(v_laura, v_t1, 0, 25, tmp_ts(-12, 6));
    -- Sofía: recién inscrita, colocación en curso
    p_participacion_temporada(v_sofia, v_t1, 0, 0, CAST(SYSTIMESTAMP - INTERVAL '2' HOUR AS TIMESTAMP));

    -- Colocaciones: puntos = mínimo(XP en la ventana, 300) / 3
    p_enfrentamiento('COLOCACION', 'FINALIZADO', tmp_ts(-15, 5), v_carlos, 240, 'COMPLETADO');
    p_enfrentamiento('COLOCACION', 'FINALIZADO', tmp_ts(-12, 6), v_laura, 120, 'COMPLETADO');
    -- Carlos (360 XP) vs Laura (135 XP): victoria +25, derrota -15
    p_enfrentamiento('ESTANDAR', 'FINALIZADO', tmp_ts(-6, 5), v_carlos, 360, 'VICTORIA', v_laura, 135, 'DERROTA');
    -- Colocación de Sofía en curso
    p_enfrentamiento('COLOCACION', 'EN_CURSO', CAST(SYSTIMESTAMP - INTERVAL '2' HOUR AS TIMESTAMP), v_sofia, NULL, NULL);

    -- ---------- Competencias por invitación ----------
    -- Duelo terminado: Carlos retó a Laura; en las 24 horas Laura sumó 180 XP (RUNNING) y Carlos 0. Laura gana +30 XP.
    INSERT INTO COMPETENCIA (id_creador, tipo, nombre, estado, duracion_ronda_horas, ronda_actual, fecha_creacion,
                             fecha_inicio, fecha_fin)
    VALUES (v_carlos, 'DUELO', 'Duelo de la semana', 'FINALIZADA', 24, 1, tmp_ts(-4, 20), tmp_ts(-3, 0), tmp_ts(-2, 0))
    RETURNING id_competencia INTO v_comp;
    p_invitado(v_comp, v_carlos, 'ACEPTADO', tmp_ts(-4, 20), tmp_ts(-4, 20), 0, 2, 'PERDEDOR');
    p_invitado(v_comp, v_laura, 'ACEPTADO', tmp_ts(-4, 20), tmp_ts(-3, 0), 180, 1, 'GANADOR');
    INSERT INTO MOVIMIENTO_XP (id_usuario, id_temporada, id_competencia, origen, cantidad, fecha_movimiento)
    VALUES (v_laura, v_t1, v_comp, 'COMPETENCIA', 30, tmp_ts(-2, 0));

    -- Grupal programada para mañana: Laura invitó a Carlos (aceptó) y a Sofía (no ha respondido).
    INSERT INTO COMPETENCIA (id_creador, tipo, nombre, duracion_ronda_horas, fecha_creacion, fecha_inicio)
    VALUES (v_laura, 'GRUPAL', 'Reto del parque', 48, tmp_ts(-1, 9), tmp_ts(1, 8))
    RETURNING id_competencia INTO v_comp;
    p_invitado(v_comp, v_laura, 'ACEPTADO', tmp_ts(-1, 9), tmp_ts(-1, 9));
    p_invitado(v_comp, v_carlos, 'ACEPTADO', tmp_ts(-1, 9), tmp_ts(-1, 10));
    p_invitado(v_comp, v_sofia, 'INVITADO', tmp_ts(-1, 9));

    -- Torneo de 3 rondas programado para dentro de 2 días: Carlos lo organiza (aceptó) e invitó a Laura y a Sofía.
    INSERT INTO COMPETENCIA (id_creador, tipo, nombre, duracion_ronda_horas, fecha_creacion, fecha_inicio)
    VALUES (v_carlos, 'TORNEO', 'Torneo del Quindío', 24, tmp_ts(-1, 8), tmp_ts(2, 8))
    RETURNING id_competencia INTO v_comp;
    p_invitado(v_comp, v_carlos, 'ACEPTADO', tmp_ts(-1, 8), tmp_ts(-1, 8));
    p_invitado(v_comp, v_laura, 'INVITADO', tmp_ts(-1, 8));
    p_invitado(v_comp, v_sofia, 'INVITADO', tmp_ts(-1, 8));

    -- Duelo rechazado: Sofía retó a Carlos y él no aceptó.
    INSERT INTO COMPETENCIA (id_creador, tipo, nombre, estado, duracion_ronda_horas, fecha_creacion)
    VALUES (v_sofia, 'DUELO', 'Primer duelo', 'CANCELADA', 24, tmp_ts(-2, 12))
    RETURNING id_competencia INTO v_comp;
    p_invitado(v_comp, v_sofia, 'ACEPTADO', tmp_ts(-2, 12), tmp_ts(-2, 12));
    p_invitado(v_comp, v_carlos, 'RECHAZADO', tmp_ts(-2, 12), tmp_ts(-2, 13));

    -- =================================================================================================
    -- MÁS DATOS DE PRUEBA (todos con contraseña Kinet2026): jugadores de distintos niveles y actividades,
    -- otro entrenador, cuentas bloqueada e inactiva, dos clanes más, ranking, enfrentamientos y competencias.
    -- =================================================================================================
    v_marta  := f_usuario('JUGADOR', 'Marta', 'Cardona', 'marta@kinet.com', 'marta_trail', 'ACTIVO', 70, DATE '1995-06-11');
    v_diego  := f_usuario('JUGADOR', 'Diego', 'Herrera', 'diego@kinet.com', 'diego_lift', 'ACTIVO', 45, DATE '1999-02-23');
    v_vale   := f_usuario('JUGADOR', 'Valentina', 'Ríos', 'valentina@kinet.com', 'vale_swim', 'ACTIVO', 30, DATE '2002-10-05');
    v_andres := f_usuario('JUGADOR', 'Andrés', 'Mejía', 'andres@kinet.com', 'andres_bici', 'ACTIVO', 90, DATE '1992-12-30');
    v_cami   := f_usuario('JUGADOR', 'Camila', 'Ortiz', 'camila@kinet.com', 'cami_cali', 'ACTIVO', 8, DATE '2005-07-19');
    v_juan   := f_usuario('JUGADOR', 'Juan', 'Torres', 'juan@kinet.com', 'juan_bloq', 'BLOQUEADO', 50, DATE '1997-03-08');
    v_lucia  := f_usuario('JUGADOR', 'Lucía', 'Pardo', 'lucia@kinet.com', 'lucia_inactiva', 'INACTIVO', 40, DATE '2000-09-14');
    v_ent2   := f_usuario('ENTRENADOR', 'Pedro', 'Salazar', 'entrenador2@kinet.com', 'entrenador_pedro', 'ACTIVO', 50, NULL);

    p_evaluacion(v_marta, 70, 'AVANZADO', 'AVANZADO', 'INTERMEDIO', 'AVANZADO', 'AVANZADO', 'AVANZADO');
    p_evaluacion(v_diego, 45, 'INTERMEDIO', 'INTERMEDIO', 'INTERMEDIO', 'AVANZADO', 'INTERMEDIO', 'INTERMEDIO');
    p_evaluacion(v_vale, 30, 'INTERMEDIO', 'INICIACION', 'INTERMEDIO', 'INTERMEDIO', 'INICIACION', 'INTERMEDIO');
    p_evaluacion(v_andres, 90, 'AVANZADO', 'AVANZADO', 'AVANZADO', 'AVANZADO', 'INTERMEDIO', 'AVANZADO');
    p_evaluacion(v_cami, 8, 'INICIACION', 'INICIACION', 'INICIACION', 'INICIACION', 'INTERMEDIO', 'INICIACION');
    p_evaluacion(v_juan, 50, 'INTERMEDIO', 'INICIACION', 'INTERMEDIO', 'INTERMEDIO', 'INTERMEDIO', 'INTERMEDIO');
    p_evaluacion(v_lucia, 40, 'INICIACION', 'INICIACION', 'INICIACION', 'INTERMEDIO', 'INICIACION', 'INICIACION');

    -- Entrenamientos (todos dentro de la Temporada 1)
    m1 := f_entreno(v_marta, 'RUNNING', tmp_ts(-17, 6), 50, 'ALTA', p_km => 9.5, p_kcal => 610);
    m2 := f_entreno(v_marta, 'RUNNING', tmp_ts(-14, 6), 45, 'MEDIA', p_km => 7, p_kcal => 470);
    m3 := f_entreno(v_marta, 'RUNNING', tmp_ts(-10, 6), 60, 'ALTA', p_km => 12, p_kcal => 800, p_notas => 'Tirada larga por el sendero');
    m4 := f_entreno(v_marta, 'FUERZA', tmp_ts(-7, 18), 40, 'MEDIA', p_kcal => 260);
    m5 := f_entreno(v_marta, 'RUNNING', tmp_ts(-4, 6), 55, 'ALTA', p_km => 10, p_kcal => 700);
    m6 := f_entreno(v_marta, 'RUNNING', tmp_ts(-1, 6), 40, 'MEDIA', p_km => 6.5, p_kcal => 430);

    d1 := f_entreno(v_diego, 'FUERZA', tmp_ts(-16, 18), 60, 'ALTA', p_kcal => 420, p_notas => 'Sentadilla y peso muerto');
    d2 := f_entreno(v_diego, 'FUERZA', tmp_ts(-13, 18), 55, 'MEDIA', p_kcal => 360);
    d3 := f_entreno(v_diego, 'FUERZA', tmp_ts(-9, 18), 70, 'ALTA', p_kcal => 500);
    d4 := f_entreno(v_diego, 'FUERZA', tmp_ts(-5, 18), 50, 'MEDIA', p_kcal => 340);
    d5 := f_entreno(v_diego, 'FUERZA', tmp_ts(-2, 18), 65, 'ALTA', p_kcal => 450);
    d6 := f_entreno(v_diego, 'FUERZA', tmp_ts(-1, 17), 200, 'MEDIA', 'PENDIENTE', p_notas => 'Doble jornada de fuerza');
    d7 := f_entreno(v_diego, 'OTRO', tmp_ts(-1, 21), 4, 'BAJA', 'RECHAZADO',
                    'La sesión debe durar al menos 10 minutos para contar.');
    UPDATE ENTRENAMIENTO SET series = 5, repeticiones = 5, peso_kg = 90 WHERE id_entrenamiento = d1;
    UPDATE ENTRENAMIENTO SET series = 4, repeticiones = 10, peso_kg = 60 WHERE id_entrenamiento = d2;
    UPDATE ENTRENAMIENTO SET series = 5, repeticiones = 3, peso_kg = 110 WHERE id_entrenamiento = d3;
    UPDATE ENTRENAMIENTO SET series = 3, repeticiones = 12, peso_kg = 50 WHERE id_entrenamiento = d4;
    UPDATE ENTRENAMIENTO SET series = 4, repeticiones = 8, peso_kg = 85 WHERE id_entrenamiento = d5;
    UPDATE ENTRENAMIENTO SET series = 5, repeticiones = 5, peso_kg = 80 WHERE id_entrenamiento = m4;

    w1 := f_entreno(v_vale, 'NATACION', tmp_ts(-15, 7), 45, 'MEDIA', p_km => 1.5, p_kcal => 400);
    w2 := f_entreno(v_vale, 'NATACION', tmp_ts(-11, 7), 50, 'MEDIA', p_km => 1.8, p_kcal => 440);
    w3 := f_entreno(v_vale, 'NATACION', tmp_ts(-8, 7), 40, 'ALTA', p_km => 1.6, p_kcal => 470);
    w4 := f_entreno(v_vale, 'NATACION', tmp_ts(-3, 7), 60, 'MEDIA', p_km => 2.2, p_kcal => 520);
    w5 := f_entreno(v_vale, 'NATACION', tmp_ts(-1, 7), 45, 'ALTA', p_km => 1.9, p_kcal => 500);

    a1 := f_entreno(v_andres, 'CICLISMO', tmp_ts(-18, 6), 120, 'ALTA', p_km => 55, p_kcal => 1500);
    a2 := f_entreno(v_andres, 'CICLISMO', tmp_ts(-14, 6), 90, 'ALTA', p_km => 40, p_kcal => 1100);
    a3 := f_entreno(v_andres, 'CICLISMO', tmp_ts(-10, 6), 150, 'MEDIA', p_km => 62, p_kcal => 1600);
    a4 := f_entreno(v_andres, 'CICLISMO', tmp_ts(-6, 6), 100, 'ALTA', p_km => 45, p_kcal => 1250);
    a5 := f_entreno(v_andres, 'CICLISMO', tmp_ts(-2, 6), 110, 'MEDIA', p_km => 48, p_kcal => 1200);

    k1 := f_entreno(v_cami, 'CALISTENIA', tmp_ts(-3, 19), 25, 'BAJA', p_kcal => 140);
    k2 := f_entreno(v_cami, 'CALISTENIA', tmp_ts(-1, 19), 30, 'MEDIA', p_kcal => 190, p_notas => 'Primeras dominadas');
    UPDATE ENTRENAMIENTO SET series = 3, repeticiones = 8 WHERE id_entrenamiento = k1;
    UPDATE ENTRENAMIENTO SET series = 4, repeticiones = 10 WHERE id_entrenamiento = k2;

    z1 := f_entreno(v_juan, 'RUNNING', tmp_ts(-18, 7), 30, 'MEDIA', p_km => 4, p_kcal => 250);
    z2 := f_entreno(v_lucia, 'CICLISMO', tmp_ts(-17, 8), 40, 'BAJA', p_km => 12, p_kcal => 220);

    -- Enlace de Strava de la sesión larga de Diego (espera revisión del entrenador)
    INSERT INTO EVIDENCIA_ENTRENAMIENTO (id_entrenamiento, url_externa, id_plataforma, fecha_subida)
    VALUES (d6, 'https://www.strava.com/activities/10345678901',
            (SELECT id_plataforma FROM PLATAFORMA_DEPORTE WHERE dominio = 'strava.com'), tmp_ts(-1, 21.5));
    -- Evidencia de Marta con enlace de Garmin, aún sin revisar
    INSERT INTO EVIDENCIA_ENTRENAMIENTO (id_entrenamiento, url_externa, id_plataforma, fecha_subida)
    VALUES (m3, 'https://connect.garmin.com/modern/activity/9876543210',
            (SELECT id_plataforma FROM PLATAFORMA_DEPORTE WHERE dominio = 'connect.garmin.com'), tmp_ts(-10, 8));

    -- Clanes
    INSERT INTO CLAN (nombre, descripcion, capacidad_max, fecha_creacion)
    VALUES ('Madrugadores', 'Entrenamos antes de las 7 a. m.', 5, tmp_ts(-16, 7)) RETURNING id_clan INTO v_clan2;
    INSERT INTO MEMBRESIA_CLAN (id_clan, id_usuario, rol_clan, fecha_ingreso) VALUES (v_clan2, v_marta, 'LIDER', tmp_ts(-16, 7));
    INSERT INTO MEMBRESIA_CLAN (id_clan, id_usuario, rol_clan, fecha_ingreso) VALUES (v_clan2, v_vale, 'MIEMBRO', tmp_ts(-14, 8));
    INSERT INTO MEMBRESIA_CLAN (id_clan, id_usuario, rol_clan, fecha_ingreso) VALUES (v_clan2, v_cami, 'MIEMBRO', tmp_ts(-2, 20));
    INSERT INTO CLAN (nombre, descripcion, capacidad_max, fecha_creacion)
    VALUES ('Fuerza Bruta', 'Pesas, barras y mucha disciplina', 10, tmp_ts(-15, 18)) RETURNING id_clan INTO v_clan3;
    INSERT INTO MEMBRESIA_CLAN (id_clan, id_usuario, rol_clan, fecha_ingreso) VALUES (v_clan3, v_diego, 'LIDER', tmp_ts(-15, 18));
    INSERT INTO MEMBRESIA_CLAN (id_clan, id_usuario, rol_clan, fecha_ingreso) VALUES (v_clan3, v_andres, 'MIEMBRO', tmp_ts(-12, 9));

    -- Retos: "Primer paso" completado por todos los que ya entrenaron
    v_part := f_participar(v_marta, 'Primer paso', tmp_ts(-18, 8), 1, 'COMPLETADO', tmp_ts(-17, 6.9));
    p_aporte(v_part, m1, 1);
    v_part := f_participar(v_diego, 'Primer paso', tmp_ts(-17, 8), 1, 'COMPLETADO', tmp_ts(-16, 19.1));
    p_aporte(v_part, d1, 1);
    v_part := f_participar(v_vale, 'Primer paso', tmp_ts(-16, 8), 1, 'COMPLETADO', tmp_ts(-15, 7.9));
    p_aporte(v_part, w1, 1);
    v_part := f_participar(v_andres, 'Primer paso', tmp_ts(-19, 8), 1, 'COMPLETADO', tmp_ts(-18, 8.1));
    p_aporte(v_part, a1, 1);
    v_part := f_participar(v_cami, 'Primer paso', tmp_ts(-4, 8), 1, 'COMPLETADO', tmp_ts(-3, 19.5));
    p_aporte(v_part, k1, 1);
    -- En curso
    v_part := f_participar(v_marta, 'Constancia de temporada', tmp_ts(-17, 8), 5, 'EN_CURSO');
    p_aporte(v_part, m1, 1); p_aporte(v_part, m2, 1); p_aporte(v_part, m3, 1); p_aporte(v_part, m5, 1); p_aporte(v_part, m6, 1);
    v_part := f_participar(v_diego, 'Constancia de temporada', tmp_ts(-16, 8), 5, 'EN_CURSO');
    p_aporte(v_part, d1, 1); p_aporte(v_part, d2, 1); p_aporte(v_part, d3, 1); p_aporte(v_part, d4, 1); p_aporte(v_part, d5, 1);
    v_part := f_participar(v_andres, 'Modo élite', tmp_ts(-18, 9), 480, 'EN_CURSO');
    p_aporte(v_part, a1, 120); p_aporte(v_part, a2, 90); p_aporte(v_part, a3, 150); p_aporte(v_part, a4, 100); p_aporte(v_part, a5, 20);
    v_part := f_participar(v_cami, 'Arranque', tmp_ts(-4, 9), 55, 'EN_CURSO');
    p_aporte(v_part, k1, 25); p_aporte(v_part, k2, 30);
    v_part := f_participar(v_marta, 'Minutos en equipo', tmp_ts(-16, 9), 80, 'EN_CURSO', NULL, v_clan2);
    p_aporte(v_part, m5, 55); p_aporte(v_part, m6, 25);
    v_part := f_participar(v_vale, 'Minutos en equipo', tmp_ts(-14, 9), 105, 'EN_CURSO', NULL, v_clan2);
    p_aporte(v_part, w4, 60); p_aporte(v_part, w5, 45);

    -- Logros
    p_logro(v_marta, 'Primeros pasos', tmp_ts(-17, 6.91));
    p_logro(v_marta, 'Espíritu de equipo', tmp_ts(-16, 7));
    p_logro(v_diego, 'Primeros pasos', tmp_ts(-16, 19.11));
    p_logro(v_diego, 'Espíritu de equipo', tmp_ts(-15, 18));
    p_logro(v_vale, 'Primeros pasos', tmp_ts(-15, 7.91));
    p_logro(v_vale, 'Espíritu de equipo', tmp_ts(-14, 8));
    p_logro(v_andres, 'Primeros pasos', tmp_ts(-18, 8.11));
    p_logro(v_andres, 'Espíritu de equipo', tmp_ts(-12, 9));
    p_logro(v_cami, 'Primeros pasos', tmp_ts(-3, 19.51));

    -- Ranking de la temporada y colocaciones
    p_participacion_temporada(v_marta, v_t1, 0, 260, tmp_ts(-17, 8));
    p_cambio_rango(v_marta, v_t1, 0, 260, tmp_ts(-8, 6));
    p_participacion_temporada(v_diego, v_t1, 0, 150, tmp_ts(-16, 8));
    p_cambio_rango(v_diego, v_t1, 0, 150, tmp_ts(-7, 18));
    p_participacion_temporada(v_vale, v_t1, 0, 120, tmp_ts(-15, 8));
    p_cambio_rango(v_vale, v_t1, 0, 120, tmp_ts(-6, 7));
    p_participacion_temporada(v_andres, v_t1, 200, 340, tmp_ts(-18, 9));
    p_cambio_rango(v_andres, v_t1, 200, 340, tmp_ts(-9, 6));
    p_enfrentamiento('COLOCACION', 'FINALIZADO', tmp_ts(-17, 8), v_marta, 330, 'COMPLETADO');
    p_enfrentamiento('COLOCACION', 'FINALIZADO', tmp_ts(-16, 8), v_diego, 300, 'COMPLETADO');
    p_enfrentamiento('COLOCACION', 'FINALIZADO', tmp_ts(-15, 8), v_vale, 240, 'COMPLETADO');
    p_enfrentamiento('ESTANDAR', 'FINALIZADO', tmp_ts(-9, 6), v_andres, 400, 'VICTORIA', v_marta, 310, 'DERROTA');
    p_enfrentamiento('ESTANDAR', 'FINALIZADO', tmp_ts(-7, 18), v_diego, 350, 'VICTORIA', v_vale, 200, 'DERROTA');

    -- Competencias: una grupal terminada y un duelo pendiente
    INSERT INTO COMPETENCIA (id_creador, tipo, nombre, estado, duracion_ronda_horas, ronda_actual, fecha_creacion,
                             fecha_inicio, fecha_fin)
    VALUES (v_marta, 'GRUPAL', 'Maratón de otoño', 'FINALIZADA', 48, 1, tmp_ts(-12, 9), tmp_ts(-10, 0), tmp_ts(-8, 0))
    RETURNING id_competencia INTO v_comp;
    p_invitado(v_comp, v_marta, 'ACEPTADO', tmp_ts(-12, 9), tmp_ts(-12, 9), 340, 1, 'GANADOR');
    p_invitado(v_comp, v_andres, 'ACEPTADO', tmp_ts(-12, 9), tmp_ts(-12, 10), 290, 2, 'PERDEDOR');
    p_invitado(v_comp, v_vale, 'ACEPTADO', tmp_ts(-12, 9), tmp_ts(-11, 8), 150, 3, 'PERDEDOR');
    INSERT INTO MOVIMIENTO_XP (id_usuario, id_temporada, id_competencia, origen, cantidad, fecha_movimiento)
    VALUES (v_marta, v_t1, v_comp, 'COMPETENCIA', 50, tmp_ts(-8, 0));
    INSERT INTO COMPETENCIA (id_creador, tipo, nombre, duracion_ronda_horas, fecha_creacion)
    VALUES (v_diego, 'DUELO', 'Duelo de pesas', 24, tmp_ts(-1, 12)) RETURNING id_competencia INTO v_comp;
    p_invitado(v_comp, v_diego, 'ACEPTADO', tmp_ts(-1, 12), tmp_ts(-1, 12));
    p_invitado(v_comp, v_andres, 'INVITADO', tmp_ts(-1, 12));

    -- Notificaciones
    p_aviso(v_marta, 'COMPETENCIA', 'Ganaste la competencia', 'Ganaste "Maratón de otoño". +50 XP.', tmp_ts(-8, 0));
    p_aviso(v_marta, 'MATCHMAKING', 'Enfrentamiento terminado', 'Perdiste contra andres_bici.', tmp_ts(-8, 6));
    p_aviso(v_marta, 'VALIDACION', 'Evidencia recibida', 'Tu enlace quedó pendiente de revisión.', tmp_ts(-10, 8), 'N');
    p_aviso(v_diego, 'VALIDACION', 'Entrenamiento en revisión',
            'Tu sesión de 200 minutos quedó pendiente de revisión por un entrenador.', tmp_ts(-1, 21.6), 'N');
    p_aviso(v_diego, 'VALIDACION', 'Entrenamiento rechazado',
            'Tu entrenamiento no fue validado: La sesión debe durar al menos 10 minutos para contar.', tmp_ts(-1, 21.1), 'N');
    p_aviso(v_andres, 'COMPETENCIA', 'Nueva invitación', 'diego_lift te invitó a "Duelo de pesas".', tmp_ts(-1, 12), 'N');
    p_aviso(v_andres, 'MATCHMAKING', 'Enfrentamiento terminado', 'Ganaste contra marta_trail.', tmp_ts(-8, 6));
    p_aviso(v_cami, 'LOGRO', 'Logro desbloqueado', 'Primeros pasos. +20 XP.', tmp_ts(-3, 19.52), 'N');
    p_aviso(v_cami, 'CLAN', 'Nuevo miembro', 'Te uniste a Madrugadores.', tmp_ts(-2, 20));
    p_aviso(v_vale, 'CLAN', 'Nuevo miembro', 'camila se unió a Madrugadores.', tmp_ts(-2, 20), 'N');

    -- Rachas
    UPDATE PERFIL_JUGADOR SET racha_actual = 1, racha_maxima = 3, fecha_ultima_actividad = TRUNC(SYSDATE) - 1 WHERE id_usuario = v_marta;
    UPDATE PERFIL_JUGADOR SET racha_actual = 2, racha_maxima = 2, fecha_ultima_actividad = TRUNC(SYSDATE) - 1 WHERE id_usuario = v_diego;
    UPDATE PERFIL_JUGADOR SET racha_actual = 1, racha_maxima = 1, fecha_ultima_actividad = TRUNC(SYSDATE) - 1 WHERE id_usuario = v_vale;
    UPDATE PERFIL_JUGADOR SET racha_actual = 1, racha_maxima = 2, fecha_ultima_actividad = TRUNC(SYSDATE) - 2 WHERE id_usuario = v_andres;
    UPDATE PERFIL_JUGADOR SET racha_actual = 2, racha_maxima = 2, fecha_ultima_actividad = TRUNC(SYSDATE) - 1 WHERE id_usuario = v_cami;

    -- ---------- Racha y XP total (suma del libro de movimientos) ----------
    UPDATE PERFIL_JUGADOR SET racha_actual = 3, racha_maxima = 3, fecha_ultima_actividad = TRUNC(SYSDATE) - 1
    WHERE id_usuario = v_laura;
    UPDATE PERFIL_JUGADOR SET racha_actual = 2, racha_maxima = 2, fecha_ultima_actividad = TRUNC(SYSDATE) - 1
    WHERE id_usuario = v_carlos;
    UPDATE PERFIL_JUGADOR SET racha_actual = 1, racha_maxima = 1, fecha_ultima_actividad = TRUNC(SYSDATE) - 1
    WHERE id_usuario = v_sofia;
    UPDATE PERFIL_JUGADOR p
    SET xp_total = (SELECT NVL(SUM(m.cantidad), 0) FROM MOVIMIENTO_XP m WHERE m.id_usuario = p.id_usuario);

    -- ---------- Token de recuperación ya usado ----------
    INSERT INTO TOKEN_RECUPERACION (id_usuario, token_hash, fecha_creacion, fecha_expiracion, fecha_uso)
    VALUES (v_laura, '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
            tmp_ts(-4, 20), tmp_ts(-4, 20.5), tmp_ts(-4, 20.2));

    -- ---------- Notificaciones ----------
    p_aviso(v_laura, 'SEGURIDAD', 'Contraseña restablecida',
            'Tu contraseña fue restablecida. Si no fuiste tú, contacta a soporte.', tmp_ts(-4, 20.2), 'S', 'EMAIL');
    p_aviso(v_laura, 'MATCHMAKING', 'Rival encontrado', 'Te enfrentas a carlos_pro durante 24 horas.', tmp_ts(-6, 5));
    p_aviso(v_laura, 'VALIDACION', 'Entrenamiento validado', 'RUNNING, 45 min. +180 XP.', tmp_ts(-3, 8));
    p_aviso(v_laura, 'RACHA', '¡Racha de 3 días!', 'Llevas 3 días seguidos entrenando.', tmp_ts(-1, 7.76), 'N');
    p_aviso(v_laura, 'RETO', 'Reto completado', 'Tu clan completó "Minutos en equipo". +150 XP.', tmp_ts(-1, 7.25), 'N');
    p_aviso(v_laura, 'VALIDACION', 'Entrenamiento rechazado',
            'Tu entrenamiento no fue validado: La sesión debe durar al menos 10 minutos para contar.', tmp_ts(-1, 20.3), 'N');
    p_aviso(v_carlos, 'CAMBIO_RANGO', 'Cambiaste de rango', 'Ahora estás en PLATA III.', tmp_ts(-5, 5));
    p_aviso(v_carlos, 'VALIDACION', 'Entrenamiento en revisión',
            'Tu sesión de 200 minutos quedó pendiente de revisión por un entrenador.', tmp_ts(-1, 20.6), 'N');
    p_aviso(v_carlos, 'RETO', 'Reto completado', 'Tu clan completó "Minutos en equipo". +150 XP.', tmp_ts(-1, 7.25),
            'N', 'EMAIL', 'FALLIDA');
    p_aviso(v_sofia, 'MATCHMAKING', 'Colocación iniciada', 'Tu enfrentamiento de colocación dura 24 horas.',
            CAST(SYSTIMESTAMP - INTERVAL '2' HOUR AS TIMESTAMP), 'N', 'PUSH', 'PENDIENTE');
    p_aviso(v_laura, 'COMPETENCIA', 'Ganaste el duelo', 'Ganaste "Duelo de la semana" contra carlos_pro. +30 XP.',
            tmp_ts(-2, 0));
    p_aviso(v_sofia, 'COMPETENCIA', 'Nueva invitación', 'laura_runner te invitó a "Reto del parque".', tmp_ts(-1, 9), 'N');
    p_aviso(v_laura, 'COMPETENCIA', 'Nueva invitación', 'carlos_pro te invitó a "Torneo del Quindío".', tmp_ts(-1, 8), 'N');
    p_aviso(v_sofia, 'COMPETENCIA', 'Nueva invitación', 'carlos_pro te invitó a "Torneo del Quindío".', tmp_ts(-1, 8), 'N');
    p_aviso(v_sofia, 'COMPETENCIA', 'Invitación rechazada', 'carlos_pro no aceptó "Primer duelo".', tmp_ts(-2, 13));

    COMMIT;
END;
/

DROP FUNCTION tmp_ts;
DROP FUNCTION tmp_id;
DROP FUNCTION tmp_rango;
DROP FUNCTION tmp_temporada;


-- =====================================================================
-- P. PERMISOS DE LOS ROLES Y SINÓNIMOS
-- =====================================================================
BEGIN
    FOR t IN (SELECT table_name FROM dba_tables WHERE owner = 'KINET_OWNER') LOOP
        -- backend: datos sí, estructura no; el historial (libro de XP y cambios de rango) es de solo inserción
        IF t.table_name IN ('MOVIMIENTO_XP', 'HISTORIAL_RANGO') THEN
            EXECUTE IMMEDIATE 'GRANT SELECT, INSERT ON kinet_owner.' || t.table_name || ' TO rol_kinet_app';
        ELSE
            EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON kinet_owner.' || t.table_name || ' TO rol_kinet_app';
        END IF;
        -- administrador funcional: todo sobre los datos
        EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON kinet_owner.' || t.table_name || ' TO rol_kinet_admin';
        -- reportes: sin credenciales ni datos personales
        IF t.table_name NOT IN ('USUARIO', 'PERFIL_JUGADOR', 'TOKEN_RECUPERACION', 'EVIDENCIA_ENTRENAMIENTO') THEN
            EXECUTE IMMEDIATE 'GRANT SELECT ON kinet_owner.' || t.table_name || ' TO rol_kinet_reader';
        END IF;
    END LOOP;
    FOR v IN (SELECT view_name FROM dba_views WHERE owner = 'KINET_OWNER') LOOP
        EXECUTE IMMEDIATE 'GRANT SELECT ON kinet_owner.' || v.view_name || ' TO rol_kinet_app';
        EXECUTE IMMEDIATE 'GRANT SELECT ON kinet_owner.' || v.view_name || ' TO rol_kinet_admin';
        EXECUTE IMMEDIATE 'GRANT SELECT ON kinet_owner.' || v.view_name || ' TO rol_kinet_reader';
    END LOOP;
    -- Sinónimos para que lectores y administradores no escriban el esquema delante de cada tabla.
    -- (El backend no los necesita: usa hibernate.default_schema=KINET_OWNER.)
    FOR o IN (SELECT object_name FROM dba_objects
              WHERE owner = 'KINET_OWNER' AND object_type IN ('TABLE', 'VIEW')) LOOP
        EXECUTE IMMEDIATE 'CREATE OR REPLACE SYNONYM kinet_admin.' || o.object_name || ' FOR kinet_owner.' || o.object_name;
        EXECUTE IMMEDIATE 'CREATE OR REPLACE SYNONYM kinet_reader.' || o.object_name || ' FOR kinet_owner.' || o.object_name;
    END LOOP;
END;
/

ALTER SESSION SET CURRENT_SCHEMA = system;

PROMPT === Kinet: base de datos creada. Usuarios: KINET_APP (backend), KINET_READER, KINET_ADMIN ===
