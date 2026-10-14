# Kinet · Backend

API REST de Kinet, hecha en Java 17 con Spring Boot 3.5 y Oracle. Implementa los procesos de negocio 1 (Gestión de Usuario y Autenticación) y 2 (Gestión de Entrenamientos y Retos).

Proyecto de Ingeniería de Software III, Universidad del Quindío.

## Cómo ejecutarlo en local

**Requisitos:** JDK 17 o superior, Docker Desktop e IntelliJ IDEA.

1. **Levantar Oracle** (solo la primera vez):
   ```bash
   docker run -d --name kinet-db -p 1522:1521 -e ORACLE_PASSWORD=admin123 -e APP_USER=kinet -e APP_USER_PASSWORD=kinet123 gvenzl/oracle-free
   ```
   Las veces siguientes basta con `docker start kinet-db`.

2. **Crear las tablas**, en orden, desde la carpeta `db` del repositorio:
   ```bash
   docker cp 01_usuarios.sql kinet-db:/tmp/
   docker cp 02_entrenamientos_retos.sql kinet-db:/tmp/
   docker exec -it kinet-db sqlplus kinet/kinet123@localhost/FREEPDB1 "@/tmp/01_usuarios.sql"
   docker exec -it kinet-db sqlplus kinet/kinet123@localhost/FREEPDB1 "@/tmp/02_entrenamientos_retos.sql"
   ```

3. **Arrancar el backend:** abrir la carpeta `backend` en IntelliJ y ejecutar `KinetApplication`. Queda en `http://localhost:8080`.

4. **Explorar la API:** `http://localhost:8080/swagger-ui.html`. Para las rutas protegidas, iniciar sesión, copiar el token y pegarlo en el botón **Authorize**.

5. **Pruebas funcionales:** abrir `kinet.http` en IntelliJ y ejecutar las peticiones en orden.

6. **Pruebas automáticas:** clic derecho sobre `src/test/java` → **Run 'All Tests'**, o `mvn test`. El reporte de cobertura queda en `target/site/jacoco/index.html`.

**Usuario administrador inicial:** `admin@kinet.com` / `Admin2026` (lo crea el script 02).

## Arquitectura

Sigue la organización por capas de las guías del curso:

```
co.edu.uniquindio.kinet
├── controllers     Reciben las peticiones HTTP y responden con ResponseDTO
├── services        Interfaces con la lógica de negocio de cada proceso
│   └── impl        Implementaciones (@Service)
├── repositories    Acceso a datos con Spring Data JPA
├── model           Entidades JPA y enumeraciones
├── dto             Objetos de entrada y salida (records con validaciones)
├── mappers         Conversión entidad a DTO con MapStruct
├── exceptions      Excepciones propias y RestExceptionHandler
├── security        Generación y validación de tokens JWT
└── config          Seguridad, CORS y Swagger
```

Todas las respuestas tienen la misma forma:

```json
{ "error": false, "respuesta": { ... } }
{ "error": true,  "respuesta": "Mensaje para el usuario" }
{ "error": true,  "respuesta": [ { "campo": "correo", "mensaje": "El correo no tiene un formato válido." } ] }
```

## Endpoints

### Proceso 1: Usuario y Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crear cuenta de jugador |
| POST | `/api/auth/login` | Iniciar sesión, devuelve el token |
| POST | `/api/auth/recuperar-contrasena` | Enviar enlace de recuperación |
| POST | `/api/auth/restablecer-contrasena` | Crear contraseña nueva con el enlace |
| GET | `/api/usuarios/me` | Ver mi perfil |
| PUT | `/api/usuarios/me` | Editar mi perfil |
| PUT | `/api/usuarios/me/contrasena` | Cambiar mi contraseña |
| GET | `/api/evaluacion/preguntas` | Preguntas de la evaluación de nivel |
| POST | `/api/evaluacion` | Enviar respuestas y recibir el nivel inicial |
| GET | `/api/evaluacion` | Ver mi resultado |

### Proceso 2: Entrenamientos y Retos

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/actividades` | Tipos de actividad y su XP por minuto |
| POST | `/api/entrenamientos` | Registrar un entrenamiento (se valida y calcula XP) |
| GET | `/api/entrenamientos` | Mi historial |
| GET | `/api/entrenamientos/{id}` | Ver un entrenamiento |
| GET | `/api/progreso` | Dashboard: XP, nivel, racha, semana y retos |
| GET | `/api/retos` | Retos disponibles para mi nivel |
| GET | `/api/retos/{id}` | Detalle de un reto con mi avance |
| POST | `/api/retos/{id}/participaciones` | Unirme a un reto |
| GET | `/api/retos/mios` | Mis participaciones |
| GET | `/api/notificaciones` | Mis notificaciones |
| PATCH | `/api/notificaciones/{id}/leida` | Marcar como leída |

### Administración (rol ADMINISTRADOR)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/admin/retos` | Todos los retos |
| POST | `/api/admin/retos` | Crear un reto |
| PATCH | `/api/admin/retos/{id}/estado?activo=false` | Activar o desactivar un reto |
| PUT | `/api/admin/actividades/{codigo}` | Cambiar el XP por minuto de una actividad |

## Reglas de negocio del Proceso 2

Están en `services/ReglasJuego.java` y tienen pruebas unitarias.

- **XP** = minutos × XP por minuto de la actividad × intensidad (baja 1, media 1.5, alta 2).
- **Niveles:** el nivel 2 pide 100 XP acumulados, el 3 pide 250, el 4 pide 450, el 5 pide 700. Cada nivel pide 50 XP más que el anterior.
- **Racha:** sube 1 si entrena el día siguiente, se mantiene si ya entrenó ese día y vuelve a 1 si pasa un día sin entrenar. Si no entrenó ni hoy ni ayer, se muestra en 0.
- **Anti-trampa:** se rechaza la sesión, guardando el motivo, si dura menos de 10 o más de 240 minutos, si está en el futuro o no ha terminado, si es de hace más de 48 horas, si se cruza con otra sesión, o si ese día ya hay 4 sesiones o se superarían 300 minutos.
- **Retos:** se ofrecen según el nivel inicial de la evaluación. Cada entrenamiento validado que se registra mientras el reto está en curso suma minutos o sesiones (solo de la actividad pedida, si el reto la especifica). Al llegar a la meta se otorga el XP de recompensa. Si vence el plazo, queda vencido y se puede intentar de nuevo.
- **Notificaciones:** se guardan en la bandeja del jugador. El envío por push está simulado en el log, listo para conectar el proveedor externo.

## Variables de entorno para desplegar

| Variable | Para qué |
|---|---|
| `DB_URL`, `DB_USUARIO`, `DB_CONTRASENA` | Conexión a Oracle |
| `KINET_JWT_SECRET` | Clave de firma de los tokens (mínimo 32 caracteres) |
| `KINET_CORS_ORIGENES` | Dirección del frontend, separadas por coma si hay varias |
| `KINET_FRONTEND_URL` | Base de los enlaces de recuperación |
| `MAIL_HOST`, `MAIL_USUARIO`, `MAIL_CONTRASENA` | Opcional, para enviar correos reales |

El `Dockerfile` de esta carpeta genera la imagen para desplegar.
