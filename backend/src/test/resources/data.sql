-- Catálogos mínimos para las pruebas (H2 en modo Oracle; las tablas las crea Hibernate).

INSERT INTO ROL (nombre, descripcion) VALUES ('JUGADOR', 'Jugador');
INSERT INTO ROL (nombre, descripcion) VALUES ('ADMINISTRADOR', 'Administrador');
INSERT INTO ROL (nombre, descripcion) VALUES ('ENTRENADOR', 'Entrenador');

INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta, activa) VALUES ('STRAVA', 'strava.com', '^/activities/[0-9]+/?$', 'S');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta, activa) VALUES ('STRAVA', 'strava.app.link', '^/[A-Za-z0-9]{6,20}$', 'S');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta, activa) VALUES ('GARMIN CONNECT', 'connect.garmin.com', '^/modern/activity/[0-9]+/?$', 'S');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta, activa) VALUES ('POLAR FLOW', 'flow.polar.com', '^/training/analysis/[0-9]+/?$', 'S');
INSERT INTO PLATAFORMA_DEPORTE (nombre, dominio, patron_ruta, activa) VALUES ('KOMOOT', 'komoot.com', '^/tour/[0-9]+/?$', 'S');

INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('INICIACION', 1, 'Empieza');
INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('INTERMEDIO', 2, 'Regular');
INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('AVANZADO', 3, 'Exigente');
INSERT INTO NIVEL_HABILIDAD (nombre, orden, descripcion) VALUES ('ATLETA', 4, 'Alto rendimiento');

-- 3 preguntas, una opción por nivel. La pregunta 2 pesa doble.
INSERT INTO PREGUNTA_EVALUACION (enunciado, orden, activa) VALUES ('¿Hace cuánto entrenas?', 1, 'S');
INSERT INTO PREGUNTA_EVALUACION (enunciado, orden, activa) VALUES ('¿Cuántas sesiones intensas haces?', 2, 'S');
INSERT INTO PREGUNTA_EVALUACION (enunciado, orden, activa) VALUES ('¿Cuánto dura tu sesión?', 3, 'S');
INSERT INTO OPCION_EVALUACION (id_pregunta, id_nivel_habilidad, titulo, descripcion, puntaje)
SELECT p.id_pregunta, n.id_nivel_habilidad, 'Opción ' || n.nombre, NULL, CASE WHEN p.orden = 2 THEN 2 ELSE 1 END
FROM PREGUNTA_EVALUACION p CROSS JOIN NIVEL_HABILIDAD n;

INSERT INTO TIPO_ACTIVIDAD (nombre, estado) VALUES ('RUNNING', 'ACTIVA');
INSERT INTO TIPO_ACTIVIDAD (nombre, estado) VALUES ('CICLISMO', 'ACTIVA');
INSERT INTO TIPO_ACTIVIDAD (nombre, estado) VALUES ('FUERZA', 'ACTIVA');
INSERT INTO TIPO_ACTIVIDAD (nombre, estado) VALUES ('CALISTENIA', 'ACTIVA');

-- XP por minuto = base x (BAJA 1, MEDIA 1.5, ALTA 2). FUERZA pide 15 minutos; el resto 10.
INSERT INTO REGLA_PUNTUACION (id_tipo_actividad, intensidad, xp_por_minuto, duracion_minima_min, vigente_desde)
SELECT t.id_tipo_actividad, i.intensidad,
       CASE t.nombre WHEN 'CICLISMO' THEN 1.5 ELSE 2 END * i.factor,
       CASE t.nombre WHEN 'FUERZA' THEN 15 ELSE 10 END,
       DATE '2026-01-01'
FROM TIPO_ACTIVIDAD t
CROSS JOIN (SELECT 'BAJA' intensidad, 1 factor FROM DUAL
            UNION ALL SELECT 'MEDIA', 1.5 FROM DUAL
            UNION ALL SELECT 'ALTA', 2 FROM DUAL) i;

INSERT INTO NIVEL_PROGRESION (numero_nivel, xp_requerido) VALUES (1, 0);
INSERT INTO NIVEL_PROGRESION (numero_nivel, xp_requerido) VALUES (2, 100);
INSERT INTO NIVEL_PROGRESION (numero_nivel, xp_requerido) VALUES (3, 250);
INSERT INTO NIVEL_PROGRESION (numero_nivel, xp_requerido) VALUES (4, 450);
INSERT INTO NIVEL_PROGRESION (numero_nivel, xp_requerido) VALUES (5, 700);
INSERT INTO NIVEL_PROGRESION (numero_nivel, xp_requerido) VALUES (6, 1000);

INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('BRONCE', 1, 1, 0);
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('PLATA', 2, 1, 1);
INSERT INTO LIGA (nombre, orden, cupos_ascenso, cupos_descenso) VALUES ('ORO', 3, 0, 1);
INSERT INTO RANGO (id_liga, division, orden, puntos_minimo)
SELECT l.id_liga, d.division, (l.orden - 1) * 3 + d.paso, ((l.orden - 1) * 3 + d.paso - 1) * 100
FROM LIGA l
CROSS JOIN (SELECT 'III' division, 1 paso FROM DUAL
            UNION ALL SELECT 'II', 2 FROM DUAL
            UNION ALL SELECT 'I', 3 FROM DUAL) d;

INSERT INTO TEMPORADA (nombre, fecha_inicio, fecha_fin, porcentaje_conservado, estado)
VALUES ('Temporada de prueba', DATEADD('DAY', -10, CURRENT_DATE), DATEADD('DAY', 50, CURRENT_DATE), 50, 'ACTIVA');

INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES ('Primeros pasos', 'Primer entrenamiento', 'ENTRENAMIENTOS>=1', 20);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES ('Racha de 3', '3 días seguidos', 'RACHA>=3', 30);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES ('Primera victoria', 'Gana un enfrentamiento', 'VICTORIAS>=1', 50);
INSERT INTO LOGRO (nombre, descripcion, criterio, xp_bonus) VALUES ('Espíritu de equipo', 'Únete a un clan', 'CLAN>=1', 25);

INSERT INTO RETO (nombre, descripcion, metrica, valor_meta, xp_recompensa, periodicidad, ambito, fecha_inicio, fecha_fin, estado)
VALUES ('Primer paso', 'Tu primer entrenamiento validado.', 'SESIONES', 1, 30, 'UNICA', 'INDIVIDUAL',
        DATEADD('DAY', -30, CURRENT_TIMESTAMP), DATEADD('DAY', 60, CURRENT_TIMESTAMP), 'ACTIVO');
INSERT INTO RETO (nombre, descripcion, metrica, valor_meta, xp_recompensa, periodicidad, ambito, fecha_inicio, estado)
VALUES ('Misión diaria', 'Entrena 30 minutos hoy.', 'MINUTOS', 30, 20, 'DIARIA', 'INDIVIDUAL',
        DATEADD('DAY', -30, CURRENT_TIMESTAMP), 'ACTIVO');
INSERT INTO RETO (id_nivel_habilidad, nombre, descripcion, metrica, valor_meta, xp_recompensa, periodicidad, ambito, fecha_inicio, estado)
VALUES ((SELECT id_nivel_habilidad FROM NIVEL_HABILIDAD WHERE nombre = 'AVANZADO'), 'Modo élite', 'Para avanzados.',
        'MINUTOS', 300, 160, 'SEMANAL', 'INDIVIDUAL', DATEADD('DAY', -30, CURRENT_TIMESTAMP), 'ACTIVO');
INSERT INTO RETO (id_tipo_actividad, nombre, descripcion, metrica, valor_meta, xp_recompensa, periodicidad, ambito, fecha_inicio, estado)
VALUES ((SELECT id_tipo_actividad FROM TIPO_ACTIVIDAD WHERE nombre = 'CALISTENIA'), 'Cien flexiones',
        'Registra 100 flexiones en el día.', 'REPETICIONES', 100, 40, 'DIARIA', 'INDIVIDUAL',
        DATEADD('DAY', -30, CURRENT_TIMESTAMP), 'ACTIVO');
INSERT INTO RETO (nombre, descripcion, metrica, valor_meta, xp_recompensa, periodicidad, ambito, fecha_inicio, fecha_fin, estado)
VALUES ('Minutos en equipo', 'Entre todo el clan sumen 60 minutos.', 'MINUTOS', 60, 50, 'UNICA', 'CLAN',
        DATEADD('DAY', -30, CURRENT_TIMESTAMP), DATEADD('DAY', 60, CURRENT_TIMESTAMP), 'ACTIVO');
INSERT INTO RETO (nombre, descripcion, metrica, valor_meta, xp_recompensa, periodicidad, ambito, fecha_inicio, fecha_fin, estado)
VALUES ('Reto en borrador', 'No visible.', 'SESIONES', 5, 50, 'UNICA', 'INDIVIDUAL',
        DATEADD('DAY', -30, CURRENT_TIMESTAMP), DATEADD('DAY', 60, CURRENT_TIMESTAMP), 'BORRADOR');
