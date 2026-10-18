INSERT INTO ROL (nombre) VALUES ('JUGADOR');
INSERT INTO ROL (nombre) VALUES ('ADMINISTRADOR');

INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('RUNNING', 'Running', 2);
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('CICLISMO', 'Ciclismo', 1);
INSERT INTO TIPO_ACTIVIDAD (codigo, nombre, xp_por_minuto) VALUES ('GIMNASIO', 'Gimnasio', 2);

INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa, activo)
VALUES ('Primer paso', 'Registra tu primer entrenamiento.', 'SESIONES', 1, NULL, NULL, 3, 30, 'S');
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa, activo)
VALUES ('Arranque', 'Suma 60 minutos en una semana.', 'MINUTOS', 60, NULL, 'INICIACION', 7, 50, 'S');
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa, activo)
VALUES ('Modo élite', 'Suma 300 minutos en una semana.', 'MINUTOS', 300, NULL, 'AVANZADO', 7, 160, 'S');
INSERT INTO RETO (titulo, descripcion, tipo_meta, meta_valor, id_tipo, nivel_requerido, duracion_dias, xp_recompensa, activo)
VALUES ('Corredor', 'Suma 90 minutos de running.', 'MINUTOS', 90, 1, NULL, 7, 80, 'S');
