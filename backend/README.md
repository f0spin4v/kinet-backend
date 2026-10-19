# Kinet · Backend

API REST de Kinet, hecha en Java 17 con Spring Boot 3.5 y Oracle. Implementa el modelo v2 completo:

- cuentas con 3 roles (jugador, administrador y entrenador);
- evaluación de nivel configurable;
- entrenamientos con cronómetro en el servidor, anti-trampa, moderación y evidencia (imagen opcional o enlace verificado de una app deportiva, con bonus de XP);
- libro de XP, niveles y rachas;
- retos por periodo y de clan;
- clanes;
- temporadas con ligas y rangos, matchmaking y enfrentamientos;
- logros, notificaciones asíncronas, soporte y analítica.

Proyecto de Ingeniería de Software III, Universidad del Quindío.

> Qué cambió respecto a v1, las correcciones al diagrama y la lógica de cada proceso: [`docs/MIGRACION_V2.md`](../docs/MIGRACION_V2.md).

## Cómo ejecutarlo en local

**Requisitos:** JDK 17 o superior, Docker Desktop e IntelliJ IDEA.

1. **Levantar Oracle** con un volumen para no perder los datos (solo la primera vez):
   ```bash
   docker run -d --name kinet-db -p 1522:1521 -e ORACLE_PASSWORD=admin123 -v kinet-db-data:/opt/oracle/oradata gvenzl/oracle-free:23-slim
   ```
   Las veces siguientes basta con `docker start kinet-db`.

2. **Crear los usuarios, las tablas y los datos** con el script de la raíz del repositorio. Corre como SYSTEM y borra y recrea todo lo de Kinet:
   ```bash
   docker cp kinet_bd.sql kinet-db:/tmp/
   docker exec -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 kinet-db sqlplus system/admin123@localhost/FREEPDB1 "@/tmp/kinet_bd.sql"
   ```
   Usuarios de base de datos que crea:

   | Usuario | Contraseña | Uso |
   |---|---|---|
   | `KINET_OWNER` | (no inicia sesión) | Dueño de las tablas |
   | `kinet_app` | `KinetApp2026` | El backend. Puede leer y escribir, pero en el libro de XP y el historial de rango solo puede insertar. |
   | `kinet_admin` | `KinetAdmin2026` | Personas del equipo: DML completo, sin DDL |
   | `kinet_reader` | `KinetLectura2026` | Reportes: solo lectura, sin datos personales |

3. **Arrancar el backend:** abrir la carpeta `backend` en IntelliJ y ejecutar `KinetApplication`. Queda en `http://localhost:8080`.

4. **Explorar la API:** `http://localhost:8080/swagger-ui.html`. Para las rutas protegidas, iniciar sesión, copiar el token y pegarlo en el botón **Authorize**.

5. **Pruebas funcionales:** abrir `kinet.http` en IntelliJ y ejecutar las peticiones en orden.

6. **Pruebas automáticas:** clic derecho sobre `src/test/java` → **Run 'All Tests'**, o `mvn test`. Usan H2 en modo Oracle: no necesitan Docker.

**Cuentas de prueba:**
- `admin@kinet.com` / `Admin2026`.
- Con la contraseña `Kinet2026`:
  - equipo: `entrenador@kinet.com`;
  - jugadores: `laura@kinet.com`, `carlos@kinet.com`, `sofia@kinet.com`, `andres@kinet.com`, `pedro@kinet.com` (este último, bloqueado).

## Arquitectura

Organización por capas, siguiendo las guías del curso:

```
co.edu.uniquindio.kinet
├── controllers     20 controladores: jugador, catálogos, admin, moderación, soporte y analítica
├── services        Lógica de negocio (@Service). ReglasJuego reúne las constantes del juego
├── repositories    Acceso a datos con Spring Data JPA
├── model           29 entidades JPA y 20 enumeraciones
├── dto             Records de entrada y salida con validaciones; cada uno sabe crearse desde su entidad: de(...)
├── exceptions      Excepciones propias y RestExceptionHandler
├── security        Generación y validación de tokens JWT
└── config          Seguridad y permisos por rol, CORS, Swagger, reloj del servidor, ejecutor asíncrono y tareas programadas
```

Todas las respuestas tienen la misma forma:

```json
{ "error": false, "respuesta": { ... } }
{ "error": true,  "respuesta": "Mensaje para el usuario" }
{ "error": true,  "respuesta": [ { "campo": "correo", "mensaje": "El correo no tiene un formato válido." } ] }
```

## Endpoints por rol

| Zona | Quién | Para qué |
|---|---|---|
| `/api/auth/**` | Público | Registro, login, recuperar y restablecer contraseña |
| `/api/usuarios/me`, `/api/progreso`, `/api/notificaciones`, `/api/catalogos/**`, `/api/temporadas/activa` | Cualquier cuenta | Perfil, dashboard, bandeja, catálogos, ranking |
| `/api/evaluacion`, `/api/entrenamientos` (cronómetro y evidencia), `/api/retos`, `/api/clanes`, `/api/matchmaking`, `/api/logros`, `/api/temporadas/activa/mia` | JUGADOR | Juego |
| `/api/moderacion/**` | ENTRENADOR o ADMINISTRADOR | Entrenamientos pendientes, evidencias, cierre de clanes, descalificaciones |
| `/api/soporte/**` | ADMINISTRADOR | Buscar cuentas, cambiar su estado, ajustes de XP |
| `/api/analitica/**` | ADMINISTRADOR | Indicadores agregados |
| `/api/admin/**` | ADMINISTRADOR | Actividades, reglas de XP, niveles, cuestionario, retos, temporadas, logros, roles |

El ADMINISTRADOR hace todo lo del equipo, pero no juega (las rutas de juego son solo de JUGADOR).

El detalle de cada endpoint está en Swagger y en la sección 5 de `docs/MIGRACION_V2.md`.

## Reglas principales

- **XP:** minutos × `xp_por_minuto` de la regla vigente para la actividad e intensidad (REGLA_PUNTUACION). Todo cambio de XP queda en MOVIMIENTO_XP; `PERFIL_JUGADOR.xp_total` es el saldo.
- **Nivel:** el mayor de NIVEL_PROGRESION cuyo XP requerido no supera el XP total (0, 100, 250, 450, 700…).
- **Cronómetro:** "Iniciar" (`POST /api/entrenamientos/en-curso`) y "Finalizar" (`POST /api/entrenamientos/en-curso/fin`).
  - Las horas las pone el servidor y la duración se calcula; el cliente nunca la envía.
  - Solo puede haber un cronómetro en marcha por jugador. Si se olvida, se cierra solo a las 5 horas.
  - Distancia y calorías son informativas: no dan XP ni cuentan para retos.
- **Anti-trampa:** una sesión se RECHAZA si dura menos que el mínimo de la regla o más de 240 minutos, o si supera 4 sesiones o 300 minutos en el día. Entre 181 y 240 minutos queda PENDIENTE para un entrenador y exige el enlace de la actividad (ver Evidencia).
- **Evidencia (RN-12):** una por entrenamiento, que puede ser una imagen (JPEG, PNG o WEBP de hasta 5 MB) o el enlace a la actividad en una app deportiva reconocida (`POST /api/entrenamientos/{id}/evidencia/enlace`).
  - Da 10 XP y queda SIN_REVISAR como respaldo. Si un entrenador la rechaza, se revierte el bonus sin dejar el XP negativo.
  - **Opcional** en entrenamientos y retos, con 48 horas para subirla.
  - **Obligatoria, y solo con enlace**, en las sesiones de 181 a 240 minutos (4 horas para subirlo; sin él el entrenador no puede aprobarla y a las 4 horas se rechaza sola) y para que un entrenamiento cuente en enfrentamientos y competencias.
  - Las apps aceptadas están en la tabla `PLATAFORMA_DEPORTE` (dominio y patrón de ruta; se agregan con un `INSERT`). El backend solo revisa la forma del enlace (https, dominio y ruta con id); no consulta a la app. Un mismo enlace no sirve dos veces.
- **Retos:** cada participación es de un periodo (el día, la semana, el reto único o la temporada). Avanzan con los minutos o sesiones de los entrenamientos validados o, en los de REPETICIONES, con aportes manuales. Los de clan se completan con la suma del clan.
- **Temporadas:** los puntos salen de los enfrentamientos de 24 horas:
  - colocación: min(XP, 300) / 3;
  - victoria: +25; empate: +10; derrota: −15.

  El rango sale de los puntos. Al cambiar de temporada se conserva un porcentaje de los puntos finales.
- **Notificaciones:** se guardan y se envían en segundo plano después del commit (PENDIENTE → ENVIADA o FALLIDA).

## Variables de entorno para desplegar

| Variable | Para qué |
|---|---|
| `DB_URL`, `DB_USUARIO`, `DB_CONTRASENA` | Conexión a Oracle (por defecto `kinet_app`) |
| `DB_ESQUEMA` | Esquema dueño de las tablas (por defecto `KINET_OWNER`) |
| `KINET_JWT_SECRET` | Clave de firma de los tokens (mínimo 32 caracteres) |
| `KINET_CORS_ORIGENES` | Dirección del frontend, separadas por coma si hay varias |
| `KINET_FRONTEND_URL` | Base de los enlaces de recuperación |
| `MAIL_HOST`, `MAIL_USUARIO`, `MAIL_CONTRASENA` | Opcional, para enviar correos reales |

El `Dockerfile` de esta carpeta genera la imagen para desplegar.
