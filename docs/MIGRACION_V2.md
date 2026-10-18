# Kinet: migración al modelo v2

Este documento explica qué cambió al pasar Kinet del modelo v1 (11 tablas, procesos 1 y 2) al modelo v2, basado en el diagrama `mermaid-diagram-2026-10-14-192336.png`. Para cada capa se describe cómo estaba antes, qué se agregó y cómo funciona ahora.

El modelo v2 tiene **34 tablas**. Empezó con 31. Después vinieron tres cambios:
- el [cronómetro en el servidor y la evidencia opcional](#0b-cambio-anterior-cronómetro-en-el-servidor-y-evidencia-opcional), que reemplazaron el GPS y los wearables (quedaron 30 tablas);
- los [usuarios de base de datos, el historial de rango, las vistas, los estados y las competencias por invitación](#0-último-cambio-usuarios-de-bd-historial-de-rango-vistas-estados-y-competencias) (33 tablas);
- los [tres roles y la evidencia con enlace verificado](#0c-último-cambio-tres-roles-y-evidencia-con-enlace-verificado) (34 tablas).

- **Base de datos:** [`kinet_bd.sql`](../kinet_bd.sql), en la raíz del repositorio.
- **Código v1 retirado:** [`docs/respaldo-v1/`](respaldo-v1/). Ahí están los controladores, servicios, modelo, DTOs, mappers, repositorios y pruebas de v1, además del `data.sql` de pruebas. Esos archivos no se compilan.
- **Optimización de consultas de los servicios (antes y después, por endpoint):** [`docs/OPTIMIZACION_CONSULTAS.md`](OPTIMIZACION_CONSULTAS.md).
- **Código retirado por el cambio del cronómetro:** [`docs/retirado-v2/`](retirado-v2/) (wearables, cifrado de tokens y registro manual).
- **Estado:** pasan las 85 pruebas sobre H2 (incluido el último cambio, sección 0c). `kinet_bd.sql` se cargó sin errores en el Oracle del Docker (34 tablas). Ver la [sección 9](#9-verificación).

---

## 0. Último cambio: usuarios de BD, historial de rango, vistas, estados y competencias

### Antes
- Todo vivía en el usuario `KINET`: dueño de las tablas y, a la vez, usuario con el que entraba el backend. Con esa conexión se podía borrar una tabla o reescribir el libro de XP.
- El rango de un jugador solo se conocía en su valor actual: no quedaba registro de cuándo subió o bajó.
- Leer el ranking o los datos públicos de un jugador obligaba a juntar varias tablas a mano.
- `NOTIFICACION.leida` era S/N: se sabía si estaba leída, pero no cuándo.
- Una actividad no se podía retirar sin borrarla (y borrarla rompía el historial).
- Una participación en temporada no tenía estado: no había forma de abandonar ni de descalificar.
- Solo existían los enfrentamientos de matchmaking (COLOCACION y ESTANDAR, emparejamiento automático). No había duelos entre amigos, competencias grupales, torneos ni invitaciones.

### Ahora
1. **Usuarios y permisos separados** (sección S del script, que ahora corre como SYSTEM).

   | Usuario | Inicia sesión | Permisos |
   |---|---|---|
   | `KINET_OWNER` | No (`NO AUTHENTICATION`) | Dueño de las tablas, vistas e índices |
   | `KINET_APP` (`KinetApp2026`) | Sí: lo usa el backend | SELECT, INSERT, UPDATE y DELETE en todo, **salvo** MOVIMIENTO_XP e HISTORIAL_RANGO: ahí solo SELECT e INSERT |
   | `KINET_ADMIN` (`KinetAdmin2026`) | Sí: personas del equipo | DML completo, sin DDL |
   | `KINET_READER` (`KinetLectura2026`) | Sí: reportes | Solo SELECT, y sin USUARIO, PERFIL_JUGADOR, TOKEN_RECUPERACION ni EVIDENCIA_ENTRENAMIENTO (datos personales) |

   - Los permisos van por roles: `ROL_KINET_APP`, `ROL_KINET_ADMIN` y `ROL_KINET_READER`.
   - Perfil `KINET_PERFIL_PERSONAS` (admin y lector): 5 intentos fallidos bloquean la cuenta 1 hora, la contraseña vence a los 180 días y no se pueden repetir las 5 últimas.
   - Perfil `KINET_PERFIL_SERVICIO` (app): sin bloqueo ni vencimiento, para que el backend no se caiga por una contraseña vencida.
   - Verificado en Docker:
     - `KINET_APP` no puede hacer UPDATE en MOVIMIENTO_XP ni DELETE en HISTORIAL_RANGO (ORA-41900), ni DROP TABLE (ORA-01031);
     - `KINET_READER` recibe ORA-00942 al leer USUARIO.
2. **HISTORIAL_RANGO.**
   - Una fila por cada cambio de rango en una temporada: la inscripción (causa INSCRIPCION, sin rango anterior) y cada vez que un enfrentamiento cambia el rango (causa ENFRENTAMIENTO).
   - Es de solo inserción.
   - Endpoint: `GET /api/temporadas/activa/mia/historial-rango`.
3. **Vistas.**
   - `V_USUARIO_PUBLICO`: gamertag, rol, estado, nivel y XP, sin correo ni contraseña.
   - `V_RANKING_TEMPORADA`: posición con `RANK()`, solo con participaciones ACTIVA o FINALIZADA.
4. **Estados nuevos.**
   - **`NOTIFICACION.fecha_lectura`** reemplaza a `leida`. NULL significa sin leer. El DTO sigue enviando `leida` (calculado) y además `fechaLectura`.
   - **`TIPO_ACTIVIDAD.estado`** (ACTIVA o INACTIVA). Una actividad INACTIVA:
     - no aparece en `/api/catalogos/actividades`;
     - no se puede usar para entrenar ni para competir;
     - conserva su historial.

     El administrador las ve todas en `GET /api/admin/actividades` y las cambia con `PATCH /api/admin/actividades/{id}/estado`.
   - **`PARTICIPACION_TEMPORADA.estado`** (ACTIVA, FINALIZADA, ABANDONADA o DESCALIFICADA):
     - Solo ACTIVA suma puntos y pide matchmaking.
     - El ranking muestra ACTIVA y FINALIZADA.
     - El jugador abandona con `POST /api/temporadas/activa/abandono`. Sale de la cola y no puede volver en esa temporada.
     - Moderación descalifica con `POST /api/moderacion/temporadas/activa/participantes/{idUsuario}/descalificacion` (`{motivo}`).
     - Al cerrar la temporada:

       | Estado al cerrar | Pasa a | Puntos finales | Posición |
       |---|---|---|---|
       | ACTIVA | FINALIZADA | Sí | Sí |
       | ABANDONADA | (sigue igual) | Sí: cuentan para el porcentaje conservado | No, y sin ascenso ni descenso |
       | DESCALIFICADA | (sigue igual) | No | No |

     - Un enfrentamiento en curso de quien abandona o es descalificado termina normal: el rival recibe sus puntos y el que salió no.
5. **Competencias por invitación** (COMPETENCIA y PARTICIPANTE_COMPETENCIA).
   - Son amistosas: no dan puntos de temporada, sino XP al ganador.

     | Tipo | Invitados | Empieza | Premio |
     |---|---|---|---|
     | DUELO | 1 | Cuando el invitado acepta. Si rechaza, o no responde en 24 horas, se cancela. | 30 XP |
     | GRUPAL | 2 a 9 | En `fechaInicio` (dentro de los próximos 7 días), si aceptaron al menos 2 jugadores contando al creador. Una sola ronda. | 50 XP |
     | TORNEO | 3 a 15 | En `fechaInicio`, si hay al menos 3 jugadores. Rondas eliminatorias. | 100 XP |

   - **Puntaje:** el XP de los entrenamientos que **empezaron** dentro de la ronda (como en los enfrentamientos). Si la competencia tiene `id_tipo_actividad`, solo cuenta esa actividad. Cada ronda dura `duracion_ronda_horas` (de 1 a 168).
   - **Torneo:** en cada ronda pasa la mitad con más XP, redondeando hacia arriba. En empate pasa quien aceptó antes. Cuando quedan 2, juegan la final como un duelo.
   - **Resultado:**
     - el mejor puntaje gana (GANADOR) y los demás pierden (PERDEDOR);
     - un empate en el primer lugar es EMPATE y cada uno recibe la mitad del premio;
     - si el mejor puntaje es 0, todos empatan y no hay premio.
   - **Posiciones:** primero quien llegó más lejos, luego el puntaje. Los empatados comparten el lugar.
   - **Invitaciones:**
     - el creador queda ACEPTADO desde el principio;
     - cada invitado pasa de INVITADO a ACEPTADO o RECHAZADO (con `fecha_respuesta`);
     - las que siguen sin respuesta cuando la competencia empieza pasan a VENCIDA.
   - **Límites:** cada jugador puede tener como máximo 3 competencias pendientes o en curso. Solo el creador cancela, y solo antes de empezar.
   - **Premio:** se registra en el libro de XP con origen COMPETENCIA. Un índice único impide premiar dos veces a la misma persona por la misma competencia.

### Cambios en la base de datos
| Objeto | Cambio |
|---|---|
| Usuarios, roles y perfiles (sección S) | Nuevos: `KINET_OWNER`, `KINET_APP`, `KINET_ADMIN`, `KINET_READER`, `ROL_KINET_*`, `KINET_PERFIL_*`. La sección P (al final) da los permisos y crea los sinónimos. |
| HISTORIAL_RANGO (nueva) | FK a PARTICIPACION_TEMPORADA. CHECK: la inscripción no tiene rango anterior; en un cambio, el rango anterior existe y es distinto del nuevo. |
| COMPETENCIA (nueva) | `tipo`, `estado`, `duracion_ronda_horas`, `ronda_actual`, fechas. CHECKs: EN_CURSO exige fechas y ronda ≥ 1; solo un torneo pasa de la ronda 1; grupales y torneos tienen `fecha_inicio`. |
| PARTICIPANTE_COMPETENCIA (nueva) | PK (competencia, usuario). `estado_invitacion`, `fecha_respuesta` (NULL solo si está INVITADO), `puntaje`, `eliminado_en_ronda`, `posicion`, `resultado` (solo si ACEPTADO). |
| MOVIMIENTO_XP | `id_competencia` y origen COMPETENCIA. La coherencia origen-referencia se partió en `ck_movimiento_origen`, `ck_movimiento_referencia` y `ck_movimiento_cantidad`. |
| NOTIFICACION | `leida` → `fecha_lectura`. Nuevo tipo COMPETENCIA. |
| TIPO_ACTIVIDAD, PARTICIPACION_TEMPORADA | Columna `estado`. |
| Vistas | `V_USUARIO_PUBLICO`, `V_RANKING_TEMPORADA`. |

**Datos de prueba nuevos:**
- Historial de rango de Carlos: T0 0→400 y T1 280→305.
- Competencias:
  - "Duelo de la semana": Laura le ganó a Carlos (+30 XP para Laura, que queda con 1000 XP).
  - "Reto del parque": grupal pendiente; Carlos aceptó y Sofía está invitada.
  - "Primer duelo": Carlos lo rechazó, así que quedó cancelado.

### Cambios en el backend
| Antes | Ahora |
|---|---|
| Conexión como `kinet`/`kinet123` | `kinet_app`/`KinetApp2026` con `hibernate.default_schema=KINET_OWNER` (variables `DB_USUARIO`, `DB_CONTRASENA`, `DB_ESQUEMA`) |
| `Notificacion.leida` | `fechaLectura`. El conteo de no leídas usa `fechaLectura IS NULL`. |
| `TemporadaService` sin estados | Estado de la participación, historial de rango, `abandonar`, `descalificar`, `exigirActiva`. El ranking y la posición solo cuentan ACTIVA y FINALIZADA. |
| `CierreTemporadaService` solo cerraba | También `abandonar` y `descalificar`, porque las dos tocan la cola de matchmaking. |
| Catálogo con todas las actividades | Al jugador solo le llegan las ACTIVAS. `CatalogoService.actividadActiva` valida al entrenar y al competir. |
| — | Entidades `HistorialRango`, `Competencia`, `ParticipanteCompetencia`; `CompetenciaService` y `CompetenciaController` (`/api/competencias`, solo JUGADOR) |
| `TareasProgramadas` | También llama a `CompetenciaService.procesar()`: vence duelos, empieza competencias y cierra rondas. Las consultas del jugador también lo llaman. |
| Analítica | Nuevo indicador `competenciasPorTipo`. |

**Endpoints de competencias:**
- `POST /api/competencias`: crear e invitar.
- `GET /api/competencias`: mis competencias.
- `GET /api/competencias/invitaciones`: invitaciones por responder.
- `GET /api/competencias/{id}`: detalle, con el puntaje en vivo.
- `POST /api/competencias/{id}/aceptacion`, `/rechazo` y `/cancelacion`.

**Pruebas nuevas (11):**
- `CompetenciaIntegracionTest`:
  - duelo con ganador y premio;
  - duelo rechazado;
  - empate sin premio;
  - grupal cancelada por falta de jugadores, con invitaciones vencidas;
  - torneo de 4 con eliminación, final y premio;
  - reglas al crear;
  - cancelación.
- Historial de rango, abandono y descalificación con cierre en `CompetitivoIntegracionTest`.
- Actividad inactiva en `EntrenamientoIntegracionTest`.

---

## 0c. Último cambio: tres roles y evidencia con enlace verificado

### Roles
Pasaron de 5 a 3. El CHECK `ck_rol_nombre` y los datos iniciales solo admiten estos:

| Rol | Qué hace |
|---|---|
| JUGADOR | Juega: entrena, compite, retos, clanes |
| ADMINISTRADOR | Todo lo del equipo: `/api/admin`, `/api/soporte`, `/api/analitica` y `/api/moderacion`. No juega |
| ENTRENADOR | El antiguo moderador: `/api/moderacion` (entrenamientos pendientes, evidencias, cierre de clanes, descalificaciones) |

MODERADOR, ANALISTA y SOPORTE desaparecieron. Las rutas `/api/soporte` y `/api/analitica` se conservan, pero ahora son solo del ADMINISTRADOR. Los tokens emitidos con un rol viejo dejan de servir: hay que iniciar sesión de nuevo. Usuarios de prueba: `admin@kinet.com` y `entrenador@kinet.com`.

### Evidencia (RN-12)
Una por entrenamiento: una **imagen** (JPEG, PNG o WEBP de hasta 5 MB) o el **enlace** a la actividad en una app deportiva reconocida. Da 10 XP y, si el entrenador la rechaza, el bonus se revierte sin dejar el XP negativo.

| Situación | Evidencia |
|---|---|
| Entrenamiento o reto (menos de 181 min) | Opcional: imagen o enlace, 48 horas desde que termina |
| Sesión de 181 a 240 min (queda PENDIENTE) | **Obligatoria y solo con enlace**, 4 horas desde que termina. El entrenador no puede aprobarla sin él; a las 4 horas sin enlace se rechaza sola |
| Que un entrenamiento cuente en un enfrentamiento o una competencia | **Obligatoria y solo con enlace** que nadie haya rechazado: sin él, esa sesión no suma al marcador |

- Los retos y las rachas no exigen evidencia.
- Si el jugador ya había subido una imagen, el enlace la reemplaza sin repetir el bonus.
- Si sube el enlace después de que cierra el enfrentamiento o la ronda, esa sesión ya no cuenta.

### Cómo se verifica el enlace
- Las apps aceptadas están en la tabla **PLATAFORMA_DEPORTE** (`nombre`, `dominio`, `patron_ruta`, `activa`). Se siembran Strava (`strava.com` y el enlace corto `strava.app.link`), Garmin Connect, Polar Flow y Komoot. Agregar o apagar una app es un `INSERT` o un `UPDATE`; no hace falta tocar el código.
- `EnlaceDeporte` exige `https`, sin usuario ni contraseña, que el host sea el dominio o un subdominio suyo y que la ruta cumpla el patrón (por ejemplo `/activities/{id}` en Strava). Guarda el enlace sin parámetros.
- Un mismo enlace no sirve para dos entrenamientos (`uq_evidencia_url`).
- **Límite:** solo se revisa la forma del enlace. No se consulta a la app, así que un enlace con un id inventado pasaría. Cerrarlo del todo requeriría la API de cada app.
- Los patrones de las semillas hay que confirmarlos con enlaces reales de cada app.

### Cambios en la base de datos
| Tabla | Cambio |
|---|---|
| PLATAFORMA_DEPORTE (nueva) | Apps reconocidas: dominio único, patrón de ruta (expresión regular) y `activa` S/N. Total: 34 tablas |
| EVIDENCIA_ENTRENAMIENTO | Nuevos `url_externa` (único) e `id_plataforma` (FK). Los campos de la imagen pasan a ser opcionales; `ck_evidencia_origen` exige imagen **o** enlace, nunca los dos |
| ROL | `ck_rol_nombre` con JUGADOR, ADMINISTRADOR y ENTRENADOR |

### Cambios en el backend
| Pieza | Cambio |
|---|---|
| `PlataformaDeporte`, `PlataformaDeporteRepository` | Entidad y lectura de las apps activas (en caché) |
| `EnlaceDeporte` | Reconoce y normaliza el enlace |
| `EvidenciaService.subirEnlace` | `POST /api/entrenamientos/{id}/evidencia/enlace`; plazo de 48 h, o de 4 h si la sesión está PENDIENTE; una imagen no vale en una sesión PENDIENTE |
| `EntrenamientoService` | `revisar` exige el enlace para aprobar; `rechazarSinEvidencia` rechaza las sesiones pendientes sin enlace pasadas las 4 horas (lo llama `TareasProgramadas`) |
| `MovimientoXpRepository` | `xpEnVentana...` solo suma entrenamientos con enlace de evidencia no rechazado: lo usan los enfrentamientos y las competencias |
| `SecurityConfig`, `Rol`, `CambiarRolDTO` | Tres roles |

### Verificación
- `kinet_bd.sql` se cargó sin errores en el Oracle del Docker (`kinet-db`): 34 tablas, 3 roles, 5 filas en PLATAFORMA_DEPORTE y la evidencia de Carlos como enlace de Strava.
- Pasan las 85 pruebas (`mvn test`). Nuevas o ampliadas: `EnlaceDeporteTest`, y casos de enlace, plazo de 4 horas, app apagada en la tabla y rechazo en `EntrenamientoIntegracionTest`; que sin enlace no suma (o deja de sumar si se rechaza) en `CompetitivoIntegracionTest` y `CompetenciaIntegracionTest`; y los permisos de los tres roles en `StaffIntegracionTest`.

---

## 0b. Cambio anterior: cronómetro en el servidor y evidencia opcional

### Antes
- El jugador **escribía** la hora de inicio y la duración del entrenamiento. Podía inventarlas.
- Las actividades marcadas con `requiere_gps` exigían una distancia.
- Las actividades también podían **importarse de wearables** (PROVEEDOR_WEARABLE, CONEXION_WEARABLE, con el token cifrado). El cliente enviaba duraciones, así que también se podían inventar.
- Como las fechas venían del cliente, el anti-trampa tenía que revisar fechas futuras, sesiones de hace más de 48 horas y cruces de horario.
- Había retos de DISTANCIA_KM y CALORIAS que usaban esos datos declarados.

### Ahora
1. **Cronómetro en el servidor.**
   - "Iniciar entrenamiento" (`POST /api/entrenamientos/en-curso`) crea la sesión EN_CURSO con la **hora del servidor**.
   - "Finalizar" (`POST /api/entrenamientos/en-curso/fin`) guarda la hora de fin del servidor y **calcula la duración**. El cliente solo envía la intensidad y, si quiere, distancia, calorías y notas.
   - Aunque el cliente mande una duración, se ignora.
   - Solo puede haber un cronómetro en marcha por jugador. Se puede cancelar sin registrar nada.
   - Si el jugador lo olvida, a las 5 horas se cierra solo como RECHAZADO.
2. **Sin GPS ni wearables.** Se quitaron `requiere_gps`, PROVEEDOR_WEARABLE, CONEXION_WEARABLE y sus endpoints. No hay mapas, permisos de ubicación ni APIs externas.
3. **Distancia y calorías opcionales e informativas.** Se guardan si el jugador las anota, pero no dan XP ni cuentan para retos. Los retos de DISTANCIA_KM y CALORIAS desaparecieron; quedan MINUTOS, SESIONES y REPETICIONES.
4. **Evidencia opcional.**
   - El jugador puede subir **una** foto o captura por entrenamiento: JPEG, PNG o WEBP de hasta 5 MB, en las 48 horas siguientes a terminarlo.
   - A cambio recibe **10 XP** (origen EVIDENCIA en el libro de XP). En un entrenamiento PENDIENTE, el bonus llega cuando el entrenador lo aprueba.
   - No se valida automáticamente: queda SIN_REVISAR como respaldo.
   - Un entrenador puede verla y aceptarla o rechazarla. Si la rechaza, el bonus se revierte con un AJUSTE.
   - El tipo de imagen se reconoce por los primeros bytes del archivo, no por lo que declare el navegador.
5. **Anti-trampa más simple.** Como las horas las pone el servidor, ya no hace falta revisar fechas futuras, antigüedad ni cruces. Quedan la duración mínima de la regla, el máximo de 240 minutos, los topes diarios y el umbral de revisión de 180 minutos.

### Cambios en la base de datos
| Tabla | Cambio |
|---|---|
| ENTRENAMIENTO | Sin `id_conexion` ni `id_actividad_externa`. `fecha_hora_fin`, `duracion_min` e `intensidad` quedan NULL mientras está EN_CURSO (`ck_entrenamiento_cronometro`). Nuevo estado EN_CURSO. `ux_entrenamiento_en_curso` permite un solo cronómetro en marcha por jugador. |
| EVIDENCIA_ENTRENAMIENTO (nueva) | Una por entrenamiento. Guarda la imagen en un BLOB, su tipo y tamaño (con CHECK), `estado_revision` (SIN_REVISAR, ACEPTADA, RECHAZADA) y la nota del entrenador. |
| MOVIMIENTO_XP | Nuevo origen EVIDENCIA con `id_evidencia` único; el CHECK de coherencia lo incluye. |
| TIPO_ACTIVIDAD | Sin `requiere_gps`. |
| RETO | `metrica` solo admite MINUTOS, SESIONES y REPETICIONES. |
| PROVEEDOR_WEARABLE, CONEXION_WEARABLE | Eliminadas. El script todavía las borra si existen de una instalación anterior. |

Datos de prueba que cambiaron:
- El reto de clan "Kilómetros en equipo" pasó a "Minutos en equipo" (360 minutos).
- Laura tiene una evidencia con su bonus, y queda con 970 XP.
- La sesión PENDIENTE de Carlos tiene una evidencia esperando la aprobación.

### Cambios en el backend
| Antes | Ahora |
|---|---|
| `POST /api/entrenamientos` con `fechaHoraInicio` y `duracionMin` | `POST /api/entrenamientos/en-curso` (iniciar), `GET` (ver los minutos transcurridos), `POST /en-curso/fin` (finalizar) y `DELETE` (cancelar) |
| `WearableService`, `WearableController`, `CifradoService` y `/api/wearables/**` | Eliminados (están en `docs/retirado-v2/`) |
| — | `EvidenciaService` y los endpoints `POST`/`GET /api/entrenamientos/{id}/evidencia`, `GET /api/moderacion/evidencias`, `GET .../{id}/archivo` y `POST .../{id}/revision` |
| `LocalDateTime.now()` en el registro | Un `Clock` inyectado (`RelojConfig`). En producción es el reloj del sistema; las pruebas lo fijan (`RelojDePrueba`) para simular una sesión de 40 minutos sin esperar. |
| Aporte a un reto si el entrenamiento se registró después de inscribirse | Si **terminó** después de inscribirse: unirse a un reto a mitad de la sesión cuenta. |
| `TareasProgramadas` vence retos y cierra enfrentamientos | Además cierra los cronómetros abandonados |

---

## 1. Cómo levantarlo

```bash
# 1. Base de datos (Oracle Free en Docker, con volumen para no perder datos)
docker run -d --name kinet-db -p 1522:1521 \
  -e ORACLE_PASSWORD=admin123 \
  -v kinet-db-data:/opt/oracle/oradata gvenzl/oracle-free:23-slim

# 2. Usuarios, esquema y datos. Corre como SYSTEM: borra y recrea KINET_OWNER, KINET_APP,
#    KINET_ADMIN y KINET_READER con todas sus tablas.
docker cp kinet_bd.sql kinet-db:/tmp/
docker exec -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 kinet-db \
  sqlplus system/admin123@localhost/FREEPDB1 @/tmp/kinet_bd.sql

# 3. Backend (desde backend/): ejecutar KinetApplication en IntelliJ, o
mvn spring-boot:run
```

El backend entra como `kinet_app` (`KinetApp2026`) y usa el esquema `KINET_OWNER`. Un contenedor creado antes con `APP_USER=kinet` conserva el usuario `KINET` con las tablas viejas: ya no se usa.

- **API:** `http://localhost:8080`.
- **Documentación interactiva:** `http://localhost:8080/swagger-ui.html`.

**Cuentas de prueba** (contraseña `Kinet2026`, salvo el administrador):

| Correo | Rol | Situación |
|---|---|---|
| admin@kinet.com (`Admin2026`) | ADMINISTRADOR | Todo lo del equipo: catálogos, retos, temporadas, roles, soporte de cuentas, analítica y moderación |
| entrenador@kinet.com | ENTRENADOR | Revisa el entrenamiento PENDIENTE de Carlos y las evidencias |
| laura@kinet.com | JUGADOR | INTERMEDIO, 1000 XP (nivel 6), racha 3, en el clan, en cola de matchmaking, una evidencia, ganó "Duelo de la semana", creó "Reto del parque" |
| carlos@kinet.com | JUGADOR | AVANZADO, 1284 XP (nivel 6), líder del clan, sesión PENDIENTE con evidencia, historial de rango |
| sofia@kinet.com | JUGADOR | INICIACION, 100 XP, colocación en curso, invitada a "Reto del parque" |
| andres@kinet.com | JUGADOR | Recién registrado, sin evaluación |
| pedro@kinet.com | JUGADOR | Cuenta BLOQUEADA |

**Propiedades nuevas** en `application.properties`:

| Propiedad | Para qué |
|---|---|
| `spring.servlet.multipart.max-file-size` / `max-request-size` | Límite de tamaño de las evidencias (5 MB / 6 MB). |
| `kinet.tareas.habilitadas`, `kinet.tareas.intervalo-ms` | Tarea periódica que cierra cronómetros abandonados, vence retos, cierra enfrentamientos y avanza las competencias (cada 60 s). |
| `spring.jpa.properties.hibernate.default_schema` (`DB_ESQUEMA`) | Esquema dueño de las tablas (`KINET_OWNER`). |
| `kinet.notificaciones.asincronas` | Ya existía. Las pruebas la ponen en `false`. |

---

## 2. Base de datos

### 2.1 Antes (v1): 11 tablas

| Tabla v1 | Contenido |
|---|---|
| ROL | JUGADOR y ADMINISTRADOR |
| USUARIO | `nombre` único, `contrasena_hash`, `acepta_datos` y `ultimo_acceso` |
| PERFIL | `nickname`, `ciudad` y `deporte_favorito` |
| TOKEN_RECUPERACION | `usado` S/N |
| EVALUACION_NIVEL | `puntaje` y `nivel_inicial` como texto. Era única por usuario y las preguntas estaban escritas en el código. |
| TIPO_ACTIVIDAD | `codigo` y `xp_por_minuto` fijo |
| PROGRESO_JUGADOR | `xp_total`, `nivel`, `racha_actual` y `racha_maxima` |
| ENTRENAMIENTO | `xp_otorgado` dentro del propio entrenamiento. Estados VALIDADO y RECHAZADO. |
| RETO | `tipo_meta` (MINUTOS o SESIONES), `duracion_dias` y `activo` S/N |
| PARTICIPACION_RETO | `progreso` |
| NOTIFICACION | `tipo` y `mensaje` |

### 2.2 Después (v2): 30 tablas (33 con el último cambio, ver sección 0)

| Área | Tablas | Novedad |
|---|---|---|
| Cuenta y seguridad | ROL, USUARIO, PERFIL_JUGADOR, TOKEN_RECUPERACION | 3 roles, nombres y apellidos, estado INACTIVO y gamertag |
| Evaluación | NIVEL_HABILIDAD, PREGUNTA_EVALUACION, OPCION_EVALUACION, EVALUACION_USUARIO, RESPUESTA_EVALUACION | El cuestionario vive en la base y la evaluación se puede repetir |
| Actividad y puntuación | TIPO_ACTIVIDAD, REGLA_PUNTUACION, NIVEL_PROGRESION | XP por actividad × intensidad con vigencia; niveles configurables |
| Entrenamiento | ENTRENAMIENTO, PLATAFORMA_DEPORTE, EVIDENCIA_ENTRENAMIENTO | Cronómetro en el servidor; evidencia con imagen o enlace verificado de una app deportiva |
| Competitivo | TEMPORADA, LIGA, RANGO, PARTICIPACION_TEMPORADA, ENFRENTAMIENTO, PARTICIPANTE_ENFRENTAMIENTO, SOLICITUD_MATCHMAKING | Temporadas, rangos por puntos y duelos de 24 horas |
| Social y retos | CLAN, MEMBRESIA_CLAN, RETO, PARTICIPACION_RETO, APORTE_RETO | Retos por periodo, de clan y con aportes |
| Recompensas | LOGRO, LOGRO_USUARIO, MOVIMIENTO_XP | Logros y libro contable de XP |
| Avisos | NOTIFICACION | Canal, título y estado de envío |

### 2.3 De dónde salió cada tabla v1

| Tabla v1 | Tabla v2 | Cambio |
|---|---|---|
| PERFIL + PROGRESO_JUGADOR | PERFIL_JUGADOR | La llave es `id_usuario`. `nickname` pasa a `gamertag`. Ya no existen `ciudad` ni `deporte_favorito`. `xp_total` y las rachas se movieron aquí. El **nivel ya no se guarda**: se calcula con NIVEL_PROGRESION. |
| EVALUACION_NIVEL | EVALUACION_USUARIO + RESPUESTA_EVALUACION | Guarda el nivel como relación con NIVEL_HABILIDAD, las respuestas elegidas y `version_algoritmo`. |
| TIPO_ACTIVIDAD.xp_por_minuto | REGLA_PUNTUACION | El XP por minuto depende de la actividad y la intensidad, con fechas de vigencia y duración mínima propia. |
| ENTRENAMIENTO.xp_otorgado | MOVIMIENTO_XP (origen ENTRENAMIENTO) | Todo el XP queda en un libro con su origen. |
| PARTICIPACION_RETO.progreso | Suma de APORTE_RETO | Se conserva `progreso` como total guardado, pero cada aporte queda registrado. |
| `usado` S/N | `fecha_uso` | Además indica cuándo se usó el token. |

### 2.4 Errores del diagrama original y cómo se corrigieron

1. **Los retos diarios y semanales no se podían repetir.** La clave única `(id_reto, id_usuario)` de PARTICIPACION_RETO impedía repetirlos. Se agregó `periodo` (el día, el lunes de la semana o el inicio del reto o de la temporada) y la clave pasó a `(id_reto, id_usuario, periodo)`.
2. **PARTICIPACION_RETO no tenía plazo.** Se agregó `fecha_limite`, que permite marcar las participaciones VENCIDO.
3. **ENTRENAMIENTO no tenía `fecha_hora_fin`.** Se agregó con `CHECK fin > inicio`. Con el cronómetro la pone el servidor al finalizar.
4. **Sumar todo el historial de XP en cada consulta era costoso.** Se agregó `PERFIL_JUGADOR.xp_total` como saldo guardado, que se actualiza en la misma transacción que el movimiento.
5. **PARTICIPACION_TEMPORADA solo tenía la foto final.** Se agregó `puntos_actuales`, que son los puntos vigentes durante la temporada.
6. **La bandeja de notificaciones no se podía ordenar.** `fecha_envio` queda NULL mientras el aviso está PENDIENTE. Se agregó `fecha_creacion`.
7. **MOVIMIENTO_XP no garantizaba la coherencia entre el origen y su referencia.** Se agregó un `CHECK` que exige exactamente una referencia según el origen: ENTRENAMIENTO, RETO, LOGRO o EVIDENCIA. AJUSTE no lleva referencia pero exige motivo.
8. **TOKEN_RECUPERACION no impedía tokens repetidos.** Se agregó `UNIQUE` en `token_hash`, además de `fecha_creacion`.
9. **El correo admitía más caracteres en la API que en la base.** La validación del registro quedó en 120 caracteres, igual que la columna.
10. **El diagrama no fijaba cómo decidir el nivel con las opciones de la evaluación.** Ahora cada opción vota por un nivel con un peso; gana el nivel con más votos y, en empate, el de menor orden.
11. **Varias reglas "solo una a la vez" no se pueden escribir como `UNIQUE` normal.** Se resolvieron con índices únicos condicionales:
    - `ux_temporada_activa`: una sola temporada ACTIVA.
    - `ux_solicitud_en_cola`: una solicitud EN_COLA por jugador y temporada.
    - `ux_membresia_vigente`: un clan a la vez por jugador.
    - `ux_clan_lider_vigente`: un único líder vigente por clan.
    - `ux_entrenamiento_en_curso`: un solo cronómetro en marcha por jugador.
    - `ux_movimiento_logro`: cada logro paga XP una sola vez.
    - `ux_aporte_entrenamiento`: cada entrenamiento aporta una sola vez a cada participación.

    Oracle no indexa las filas en las que la expresión es NULL, así que la regla solo aplica a las filas que cumplen la condición.

12. **No se podían registrar dos aportes manuales al mismo reto.** Se corrigió en el script ya migrado. Una `UNIQUE (id_participacion_reto, id_entrenamiento)` normal trataba dos aportes manuales de la misma participación como duplicados, porque Oracle compara `(participación, NULL)` como igual. Eso rompía los retos de REPETICIONES. Se cambió por `ux_aporte_entrenamiento`.

### 2.5 Organización del script

`kinet_bd.sql` está dividido en estas secciones:

0. Limpieza: borra las tablas v1 y v2 si existen, así el script se puede repetir.
1. Cuenta y seguridad.
2. Evaluación.
3. Actividad y puntuación.
4. Competitivo.
5. Social y retos.
6. Recompensas.
7. Notificaciones.
8. Datos iniciales: los 5 roles, 4 niveles de habilidad, el cuestionario de 5 preguntas, 6 actividades con 18 reglas, 30 niveles, 6 ligas con 16 rangos, 2 temporadas, 8 logros, 10 retos y el administrador.
9. Datos de prueba.

Los datos de prueba usan fechas relativas a `SYSDATE` y quedan coherentes con las reglas del backend:

- `xp_total` es la suma de MOVIMIENTO_XP.
- El avance de cada reto es la suma de sus aportes.
- Los puntos de temporada vienen de los enfrentamientos.

---

## 3. Backend: estructura

### 3.1 Antes (v1)

```text
controllers/   9 controladores (auth, usuario, evaluación, actividad, entrenamiento, reto, progreso, notificación, admin)
services/      interfaces + services/impl/ (una implementación por interfaz)
mappers/       MapStruct (UsuarioMapper, RetoMapper, EntrenamientoMapper, NotificacionMapper)
model/         11 entidades + enums (NivelInicial, TipoMeta, EstadoEntrenamiento…)
Cuestionario.java  5 preguntas fijas en el código (A = 0, B = 1)
```

### 3.2 Después (v2)

```text
controllers/   20 controladores, 85 endpoints (jugador, catálogos, admin, moderación, soporte, analítica)
services/      16 servicios concretos @Service + ReglasJuego + CriterioLogro + Notificacion{Evento,Listener}
dto/           60 records agrupados por módulo, cada uno con su método estático de(...) para mapear
model/         29 entidades + RESPUESTA_EVALUACION como @ManyToMany + 20 enums
repositories/  29 repositorios + la proyección Conteo para analítica
config/        AsyncConfig, SecurityConfig (permisos por rol), OpenApiConfig, RelojConfig, TareasProgramadas
security/      JwtUtils, JwtFiltro
```

**Qué se conservó sin cambios de comportamiento:**
- JWT y BCrypt.
- `ResponseDTO`, `RestExceptionHandler` y las excepciones.
- `SiNoConverter`.
- El envío de correo. `EmailService` ganó el método `enviarAviso`.
- Las notificaciones asíncronas: el evento, `AFTER_COMMIT` y el pool `notificacion-*`.
- El dashboard en una sola consulta JPQL.

**Qué cambió en la forma del código:**

| Antes | Ahora | Por qué |
|---|---|---|
| Interfaz + `impl` para cada servicio | Clase `@Service` concreta | Ninguna interfaz tenía una segunda implementación. Se conserva la interfaz solo en `EmailService`, donde sí hay un reemplazo esperado: el SMTP real. |
| MapStruct | `XxxDTO.de(entidad)` en cada record | Los DTO v2 combinan datos de varias entidades (gamertag, rango, XP del libro) y el mapeo explícito queda más claro. |
| `Cuestionario.java` | Tablas PREGUNTA_EVALUACION y OPCION_EVALUACION | El administrador edita el cuestionario sin desplegar. |
| `ReglasJuego.calcularXp(min, xpPorMinuto, intensidad)` con multiplicador en el enum | `ReglasJuego.calcularXp(min, regla.xpPorMinuto)` | El multiplicador por intensidad ya está dentro de cada regla. |
| `ReglasJuego.nivelParaXp` con fórmula | `CatalogoService.nivelPara(xp)` leyendo NIVEL_PROGRESION | Los niveles son configurables. |
| Listener con `@Transactional(REQUIRES_NEW)` | Listener con `TransactionTemplate` `REQUIRES_NEW` en dos pasos: guardar PENDIENTE y luego ENVIADA o FALLIDA | Si el proceso se cae a mitad del envío, el aviso queda PENDIENTE y no se pierde. |

---

## 4. Lógica resultante por proceso

### 4.1 Cuentas, roles y estados
- **Registro.** Crea USUARIO (rol JUGADOR) y PERFIL_JUGADOR en cero. El correo se normaliza a minúsculas y es único. El gamertag también es único.
- **Login.** Una cuenta BLOQUEADA o INACTIVA recibe 403 con un mensaje distinto para cada caso. Si la contraseña es incorrecta, la respuesta no revela si el correo existe.
- **El jugador desactiva su cuenta.** `DELETE /api/usuarios/me` la deja INACTIVA. Soporte puede reactivarla.
- **Recuperación de contraseña.** Al pedir un enlace nuevo se invalidan los anteriores, marcando su `fecha_uso`. El token se guarda como SHA-256 y vence a los 30 minutos. Al restablecer, se envía un aviso de SEGURIDAD por EMAIL.
- **Roles:**

  | Rol | Puede hacer |
  |---|---|
  | JUGADOR | Juega |
  | ADMINISTRADOR | `/api/admin/**`, `/api/soporte/**`, `/api/analitica/**` y `/api/moderacion/**` (todo lo del equipo) |
  | ENTRENADOR | `/api/moderacion/**` |

  El staff no puede usar las rutas de juego (403).
- **Soporte (del administrador):** nadie puede cambiar el estado de su propia cuenta.

### 4.2 Evaluación de nivel (Placement)
1. `GET /api/evaluacion/preguntas` devuelve las preguntas activas, sin decir a qué nivel vota cada opción.
2. El jugador envía una opción por cada pregunta activa. Si sobran o faltan opciones, la respuesta es 400.
3. Se suman los pesos por nivel. Gana el nivel con más votos; en empate, el de menor orden.
4. Se guardan la evaluación, las respuestas y `version_algoritmo = VOTO_PONDERADO_V1`.
5. Se puede repetir a los 30 días. El nivel vigente es el de la evaluación más reciente.
6. El administrador crea, edita y desactiva preguntas. Las opciones de una pregunta que ya tiene respuestas no se pueden cambiar, para no alterar el historial.

### 4.3 Entrenamientos: cronómetro, anti-trampa y evidencia
Flujo: **iniciar → finalizar → anti-trampa → XP → racha → retos → evidencia → logros → notificación**.

**Cronómetro:**
- **Iniciar** registra la actividad y la hora de inicio del servidor (EN_CURSO). Si ya hay uno en marcha, la respuesta es 409.
- **Finalizar** registra la hora de fin del servidor, calcula la duración en minutos y pide la intensidad. Distancia, calorías y notas son opcionales e informativas.
- **Cancelar** borra el cronómetro sin registrar nada.
- **Cierre automático:** un cronómetro que sigue corriendo a las 5 horas se cierra como RECHAZADO.
- Al finalizar debe existir una regla vigente para la actividad e intensidad en la fecha de inicio.

**Reglas del anti-trampa**, en este orden:

| Regla | Resultado |
|---|---|
| Duración menor a `regla.duracion_minima_min` (FUERZA pide 15 min; el resto, 10) | RECHAZADO |
| Más de 240 min ("¿olvidaste finalizar el cronómetro?") | RECHAZADO |
| Más de 4 sesiones o 300 min en el día | RECHAZADO |
| Más de 180 min (hasta 240) | **PENDIENTE**, para que lo revise un entrenador; exige el enlace de la actividad (4 horas para subirlo) |

**Cuando queda VALIDADO:**
- El XP es `ROUND(minutos × xp_por_minuto)`.
- Se registra un MOVIMIENTO_XP de origen ENTRENAMIENTO y se actualiza el saldo del perfil.
- Si sube de nivel, se notifica NIVEL.

**Revisión del entrenador:**
- Si aprueba, se otorga el XP con la regla vigente en la fecha de la sesión, más el bonus de la evidencia si la tiene. **No puede aprobar una sesión sin el enlace de la actividad** (ver la [sección 0c](#0c-último-cambio-tres-roles-y-evidencia-con-enlace-verificado)).
- Si rechaza, el motivo es obligatorio.

**Evidencia (RN-12):** una por entrenamiento VALIDADO o PENDIENTE; ver la [sección 0c](#0c-último-cambio-tres-roles-y-evidencia-con-enlace-verificado) para las reglas completas.
- Imagen (JPEG, PNG o WEBP de hasta 5 MB) o enlace a la actividad en una app deportiva reconocida.
- Da 10 XP (origen EVIDENCIA). Si el entrenamiento está PENDIENTE, el bonus llega cuando se aprueba.
- Queda SIN_REVISAR. El entrenador la ve en `GET /api/moderacion/evidencias` y la acepta o la rechaza con una nota.
- Si la rechaza, se revierte el bonus con un AJUSTE negativo, sin dejar el XP por debajo de 0, y se avisa al jugador.

### 4.4 XP, nivel y racha
- **Libro de XP.** Todo el XP entra por `ProgresoService` y siempre deja un MOVIMIENTO_XP con su origen: ENTRENAMIENTO, RETO, LOGRO, EVIDENCIA o AJUSTE.
- **Ajustes de soporte.** Pueden restar, pero el saldo nunca queda negativo y el motivo es obligatorio.
- **Nivel.** Es el `numero_nivel` mayor cuyo `xp_requerido` no supera el XP total: 0, 100, 250, 450, 700, 1000…
- **Racha.** El primer día cuenta 1. El mismo día se mantiene, el día siguiente suma 1 y un hueco la reinicia en 1. Desde 3 días se avisa cada vez que aumenta. En el dashboard se muestra 0 si el jugador no entrenó ni hoy ni ayer.

### 4.5 Retos
- **Visibilidad.** El jugador ve los retos ACTIVOS y vigentes que no piden nivel, más los de su nivel vigente. Los de TEMPORADA solo aparecen si su temporada está activa.
- **Periodo:**

  | Periodicidad | Inicio del periodo | Límite |
  |---|---|---|
  | DIARIA | El día | El día siguiente |
  | SEMANAL | El lunes | 7 días después |
  | UNICA | El inicio del reto | `fecha_fin` del reto, o inscripción + 30 días |
  | TEMPORADA | El inicio de la temporada | Fin de la temporada + 1 día |

  Si el reto tiene `fecha_fin`, el límite nunca la supera. Unirse dos veces al mismo reto en el mismo periodo da 409.
- **Aportes automáticos.** Al validar un entrenamiento, este cuenta para un reto en curso si cumple tres condiciones: empezó dentro del periodo, **terminó** después de la inscripción (unirse a mitad de la sesión cuenta) y es de la actividad pedida (si el reto pide una).
- **Cantidad que aporta según la métrica:**

  | Métrica | Aporta |
  |---|---|
  | MINUTOS | La duración |
  | SESIONES | 1 |
  | REPETICIONES | Nada automático: solo aportes manuales |

- **Completar.** En un reto INDIVIDUAL, el reto se completa cuando el progreso propio llega a la meta. En un reto de CLAN se completa cuando la suma del clan en ese periodo llega a la meta; entonces se completan todas las participaciones del clan y cada miembro recibe el XP.
- **Vencer.** Las participaciones con el plazo cumplido quedan VENCIDAS. Las vence la tarea programada y también cada consulta del jugador.
- **Abandono.** El jugador puede abandonar un reto en curso.
- **Administrador.** El ciclo es BORRADOR → ACTIVO → FINALIZADO. Solo se edita un reto en BORRADOR, y al finalizarlo sus participaciones en curso vencen.

### 4.6 Clanes
- Un jugador pertenece a un solo clan a la vez. Crear un clan lo deja como LIDER.
- Unirse a un clan respeta su capacidad y avisa al líder.
- Si sale el líder, el liderazgo pasa al miembro más antiguo. Si sale el último miembro, el clan se cierra.
- El líder puede expulsar miembros, transferir el liderazgo, editar el clan y cerrarlo. Moderación también puede cerrar cualquier clan.
- Al salir de un clan, sus retos de clan en curso quedan ABANDONADOS.

### 4.7 Temporadas, ligas y rangos
- **Inscripción.** Ocurre al pedir el primer enfrentamiento de la temporada.
- **Puntos iniciales.** Son `puntos_finales` de la última temporada cerrada × `porcentaje_conservado` de esa temporada.
- **Ascenso y descenso.** Solo aplican en ligas con más jugadores que cupos, para que nadie suba y baje a la vez:
  - Los `cupos_ascenso` mejores arrancan como mínimo en el primer rango de la liga superior.
  - Los `cupos_descenso` peores arrancan como máximo en el último rango de la liga inferior.
- **Rango.** Es el de mayor `puntos_minimo` que no supera los puntos. Hay 100 puntos por rango, de BRONCE III (0) a ELITE (1500). Al cambiar de rango se notifica CAMBIO_RANGO.
- **Administrador.**
  - Crea y edita temporadas en estado PROGRAMADA.
  - Activa una temporada, siempre que no haya otra activa.
  - Cierra la temporada activa: se cancelan los enfrentamientos en curso y la cola, se guarda la foto final (`puntos_finales` y `posicion_final`) y se notifica FIN_TEMPORADA.

### 4.8 Matchmaking y enfrentamientos
- **Solicitud.** No se puede pedir un enfrentamiento si ya hay uno en curso o si el jugador ya está en la cola.
- **Colocación.** El primer enfrentamiento de la temporada es una COLOCACION individual de 24 horas. Al terminar da `min(XP, 300) / 3` puntos.
- **Emparejamiento.** Después de la colocación, el jugador entra a la cola y se empareja con el rival de rango más parecido; en empate, con el que lleva más tiempo esperando. El duelo ESTANDAR dura 24 horas.
- **Puntaje.** Es el XP de los entrenamientos que empezaron dentro de la ventana. Mientras el enfrentamiento sigue en curso se calcula en vivo.
- **Resultado del duelo:**

  | Resultado | Puntos de temporada |
  |---|---|
  | Victoria | +25 |
  | Empate | +10 |
  | Derrota | −15, sin bajar de 0 |

- **Cierre.** Los enfrentamientos se cierran con la tarea programada o al consultarlos. Al cerrar se evalúan los logros (por ejemplo VICTORIAS).

### 4.9 Logros
- El criterio se guarda como texto `METRICA>=VALOR`. Las métricas son ENTRENAMIENTOS, MINUTOS, RACHA, XP, NIVEL, RETOS, VICTORIAS y CLAN.
- Los logros se evalúan:
  - después de validar un entrenamiento,
  - al hacer un aporte manual,
  - al crear un clan o unirse a uno,
  - al cerrar un enfrentamiento.
- La evaluación se repite mientras aparezcan logros nuevos, porque el XP de un logro puede hacer cumplir otro de NIVEL o XP.
- El administrador no puede borrar un logro que ya se otorgó.

### 4.10 Notificaciones
- `notificar()` publica un evento y regresa enseguida.
- Después del commit, en el pool `notificacion-*`:
  1. Se guarda el aviso como PENDIENTE.
  2. Se envía: por push (simulado en el log) o por EMAIL con `EmailService`.
  3. Queda ENVIADA o FALLIDA, con `fecha_envio`.
- Si la operación que originó el aviso se deshace, el aviso no se crea.
- Tipos: VALIDACION, RACHA, NIVEL, RETO, LOGRO, CLAN, MATCHMAKING, CAMBIO_RANGO, FIN_TEMPORADA, RECUPERACION y SEGURIDAD.

### 4.11 Wearables (retirado)
Se quitaron con el cambio del cronómetro (ver la [sección 0](#0-último-cambio-cronómetro-en-el-servidor-y-evidencia-opcional)). Ahora se aporta el enlace de la actividad en Strava, Garmin Connect, Polar Flow o Komoot como evidencia (ver la sección 0c), o una captura como evidencia opcional.

### 4.12 Analítica
`GET /api/analitica/resumen` solo devuelve conteos, sin datos personales:
- usuarios por estado y por rol;
- jugadores por nivel de habilidad vigente;
- entrenamientos por estado, minutos por actividad, XP por origen y retos completados en los últimos 30 días;
- jugadores por liga en la temporada activa;
- notificaciones por estado de envío.

### 4.13 Tareas programadas
`TareasProgramadas` corre cada 60 segundos. Cierra los cronómetros abandonados (más de 5 horas), vence las participaciones de retos y cierra los enfrentamientos cuya ventana terminó. Las pruebas la apagan con `kinet.tareas.habilitadas=false`.

---

## 5. Endpoints

| Zona | Rol | Endpoints |
|---|---|---|
| `/api/auth` | Público | `POST registro`, `POST login`, `POST recuperar-contrasena`, `POST restablecer-contrasena` |
| `/api/usuarios/me` | Autenticado | `GET`, `PUT`, `PUT contrasena`, `DELETE` (desactivar) |
| `/api/catalogos` | Autenticado | `GET actividades`, `niveles-habilidad`, `niveles-progresion`, `ligas` |
| `/api/progreso` | Autenticado | `GET` (dashboard), `GET movimientos` |
| `/api/notificaciones` | Autenticado | `GET`, `PATCH {id}/leida`, `PATCH leidas` |
| `/api/temporadas/activa` | Autenticado / JUGADOR | `GET`, `GET ranking`, `GET mia`, `GET mia/historial-rango` y `POST abandono` (JUGADOR) |
| `/api/evaluacion` | JUGADOR | `GET preguntas`, `POST`, `GET`, `GET historial` |
| `/api/entrenamientos` | JUGADOR | `POST en-curso` (iniciar), `GET en-curso`, `POST en-curso/fin` (finalizar), `DELETE en-curso` (cancelar), `GET`, `GET {id}`, `POST {id}/evidencia` (multipart, campo `archivo`), `POST {id}/evidencia/enlace` (`{"url"}`), `GET {id}/evidencia` (imagen) |
| `/api/retos` | JUGADOR | `GET`, `GET mios`, `GET {id}`, `POST {id}/participaciones`, `POST participaciones/{id}/aportes`, `POST participaciones/{id}/abandono` |
| `/api/clanes` | JUGADOR | `GET`, `POST`, `GET mio`, `POST mio/salida`, `GET {id}`, `PUT {id}`, `POST {id}/miembros`, `DELETE {id}/miembros/{idUsuario}`, `PUT {id}/lider`, `POST {id}/cierre` |
| `/api/matchmaking` | JUGADOR | `POST solicitudes`, `DELETE solicitudes/actual`, `GET enfrentamientos`, `GET enfrentamientos/{id}` |
| `/api/competencias` | JUGADOR | `POST`, `GET`, `GET invitaciones`, `GET {id}`, `POST {id}/aceptacion`, `POST {id}/rechazo`, `POST {id}/cancelacion` |
| `/api/logros` | JUGADOR | `GET` |
| `/api/admin` | ADMINISTRADOR | Actividades (`POST`, `PUT`); reglas de puntuación (`GET`, `POST`); niveles de progresión (`PUT`); preguntas (`GET`, `POST`, `PUT`, `PATCH estado`); retos (`GET`, `POST`, `PUT`, `PATCH estado`); temporadas (`GET`, `POST`, `PUT`, `POST activacion`, `POST cierre`); logros (`POST`, `PUT`, `DELETE`); usuarios (`PATCH rol`) |
| `/api/moderacion` | ENTRENADOR / ADMINISTRADOR | `GET entrenamientos/pendientes`, `POST entrenamientos/{id}/revision`, `GET evidencias?estado=`, `GET evidencias/{id}/archivo`, `POST evidencias/{id}/revision`, `POST clanes/{id}/cierre` |
| `/api/soporte` | ADMINISTRADOR | `GET usuarios`, `PATCH usuarios/{id}/estado`, `POST ajustes-xp` |
| `/api/analitica` | ADMINISTRADOR | `GET resumen` |

---

## 6. Cambios de contrato para el frontend

| v1 | v2 |
|---|---|
| Registro: `nombre`, `nickname`, `aceptaDatos` | `nombres`, `apellidos`, `gamertag`, `fechaNacimiento` (opcional), `aceptaPolitica` |
| `UsuarioDTO.nickname`, `ciudad`, `deporteFavorito`, `nivelInicial` | `gamertag`, `estado` y `nivelHabilidad`. Se quitaron `ciudad` y `deporteFavorito`. |
| Evaluación: `{"respuestas": ["A","B",...]}` | `{"idsOpciones": [ids de PREGUNTA.opciones]}`. Las preguntas se piden a `GET /api/evaluacion/preguntas`. |
| Entrenamiento: `POST /api/entrenamientos` con `codigoActividad`, `fechaInicio` y `duracionMinutos` | Botones de cronómetro: `POST /api/entrenamientos/en-curso` con `{idTipoActividad}` y luego `POST /api/entrenamientos/en-curso/fin` con `{intensidad, distanciaKm?, caloriasKcal?, notas?}`. El cliente no envía horas ni duración. El DTO trae `minutosTranscurridos` mientras corre y `evidencia` con el estado de la foto. |
| — | Evidencia: `POST /api/entrenamientos/{id}/evidencia` (multipart/form-data, campo `archivo`) o `POST /api/entrenamientos/{id}/evidencia/enlace` (`{"url"}`). `EvidenciaDTO` ahora trae `tipo` (IMAGEN o ENLACE), `url` y `plataforma` |
| `GET /api/actividades` | `GET /api/catalogos/actividades`, con `reglasVigentes` |
| Reto: `titulo`, `tipoMeta`, `metaValor`, `duracionDias`, `activo` | `nombre`, `metrica`, `valorMeta`, `periodicidad`, `ambito`, `estado`, `fechaInicio` y `fechaFin` |
| Notificación: `tipo`, `mensaje`, `fecha` | Además `canal`, `titulo`, `estadoEnvio`, `fechaCreacion` y `fechaEnvio` |
| `PUT /api/admin/actividades/{codigo}` (XP por minuto) | `POST /api/admin/reglas-puntuacion`, que crea una regla con vigencia |

---

## 7. Pruebas

Corren con `mvn test` sobre H2 en modo Oracle. Las tablas las crea Hibernate a partir de las entidades y los catálogos salen de `src/test/resources/data.sql`. El cronómetro se prueba con `RelojDePrueba`, un reloj que las pruebas fijan para simular las horas de inicio y fin. En total son 73 pruebas en 14 clases (las 11 del último cambio están descritas en la sección 0; `ConsultasPorPeticionTest` limita las consultas por endpoint):

| Clase | Cubre |
|---|---|
| `ReglasJuegoTest` | XP, colocación, racha y criterio de logros (pruebas unitarias) |
| `AuthIntegracionTest` | Registro, unicidad, validaciones, login por estado y recuperación de un solo uso |
| `PerfilIntegracionTest` | Editar el perfil, contraseña y desactivación |
| `EvaluacionIntegracionTest` | Voto ponderado, empate, respuestas incompletas y repetición a los 30 días |
| `EntrenamientoIntegracionTest` | Cronómetro (duración calculada por el servidor, uno a la vez, cancelar, cierre automático), XP por regla, libro = saldo, anti-trampa, tope diario, PENDIENTE con aprobación y rechazo, evidencia (tipo, bonus, una sola vez, plazo, rechazo que revierte el bonus), racha y logros |
| `RetoIntegracionTest` | Visibilidad por nivel, avance, periodo único, REPETICIONES manual, reto de clan por minutos (la distancia no cuenta), abandono y ciclo de administración |
| `ClanIntegracionTest` | Líder, capacidad, sucesión del liderazgo, expulsión, cierre y moderación |
| `CompetitivoIntegracionTest` | Colocación, emparejamiento, victoria y derrota, ranking, cola, cierre de temporada y porcentaje conservado |
| `StaffIntegracionTest` | Permisos por rol, soporte, reglas con vigencia, cambio de rol y administración de logros |
| `ProgresoIntegracionTest` | Dashboard en una consulta, bandeja de notificaciones y catálogos |
| `NotificacionAsincronaTest` | Pool propio, envío en segundo plano y que no haya aviso si la operación se deshace |

---

## 8. Pendientes y limitaciones conocidas

- **Push y correo simulados.** El push se escribe en el log. El correo es real solo si se configura `MAIL_HOST`.
- **Evidencias en la base de datos.** Las imágenes se guardan como BLOB para que el entorno sea autocontenido. Con muchos usuarios convendría moverlas a un almacenamiento de objetos y guardar solo la referencia.
- **Reportes de jugadores.** La evidencia queda lista para revisar cuando alguien es reportado, pero todavía no existe un flujo para que un jugador reporte a otro.
- **Enfrentamientos.** No hay revancha ni bloqueo de rivales repetidos.
- **Frontend.** No está en el repositorio y no se modificó. Necesita ajustarse a la [sección 6](#6-cambios-de-contrato-para-el-frontend).

## 9. Verificación

- `mvn test`: pasan las 73 pruebas.
- `kinet_bd.sql` se cargó sin errores en Oracle Free 23ai. Quedaron 30 tablas, y para laura, carlos y sofia el XP total (970 / 1284 / 100) coincide con la suma de su libro de XP.
- El backend arrancó contra ese Oracle con `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`: Hibernate confirmó que las entidades coinciden con las tablas.
- Se recorrió el flujo nuevo contra Oracle con los datos de prueba:
  - iniciar y no poder iniciar otro (409), ver los minutos transcurridos;
  - finalizar enseguida (RECHAZADO por la duración mínima);
  - subir una evidencia (+10 XP);
  - el entrenador ve los pendientes y las evidencias, descarga la imagen y aprueba la sesión de 200 minutos de Carlos (+600 XP y +10 del bonus de su evidencia);
  - el entrenador rechaza la evidencia de Laura (se revierten los 10 XP con un AJUSTE);
  - `/api/wearables` ya no existe.

  Después se recargó el script para dejar la base con los datos de prueba originales.
