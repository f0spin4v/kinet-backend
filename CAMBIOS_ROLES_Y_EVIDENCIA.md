# Cambios: tres roles y evidencia con enlace verificado

Resumen de una página. El detalle está en la sección 0c de [`docs/MIGRACION_V2.md`](docs/MIGRACION_V2.md).

## 1. Roles (de 5 a 3)

| Rol | Qué hace |
|---|---|
| JUGADOR | Juega: entrena, compite, retos, clanes. |
| ADMINISTRADOR | Todo lo del equipo: `/api/admin`, `/api/soporte`, `/api/analitica` y `/api/moderacion`. No juega. |
| ENTRENADOR | El antiguo moderador: `/api/moderacion` (entrenamientos pendientes, evidencias, cierre de clanes, descalificaciones). |

- Desaparecen MODERADOR, ANALISTA y SOPORTE. Las rutas `/api/soporte` y `/api/analitica` siguen, pero ahora son solo del ADMINISTRADOR.
- Los tokens emitidos con un rol viejo dejan de servir: hay que iniciar sesión otra vez.
- Cuentas de prueba del equipo: `admin@kinet.com` y `entrenador@kinet.com`.

## 2. Evidencia (RN-12)

Una por entrenamiento: una imagen (JPEG, PNG o WEBP, hasta 5 MB) o el enlace a la actividad en una app deportiva reconocida. Da 10 XP; si el entrenador la rechaza, el bonus se revierte sin dejar el XP negativo.

| Situación | Evidencia |
|---|---|
| Entrenamiento o reto (hasta 180 min) | Opcional, imagen o enlace, 48 horas. |
| Sesión de 181 a 240 min | **Obligatoria, solo enlace**, 4 horas. Sin él, el entrenador no puede aprobarla y a las 4 horas se rechaza sola. |
| Contar en un enfrentamiento o competencia | **Obligatoria, solo enlace** no rechazado. Sin él, la sesión no suma al marcador. |

- Las apps aceptadas están en la tabla `PLATAFORMA_DEPORTE` (Strava, Garmin Connect, Polar Flow, Komoot). Agregar una es un `INSERT`, sin tocar código.
- Solo se revisa la **forma** del enlace (https, dominio y ruta con id). No se consulta a la app, así que un id inventado pasaría. Un mismo enlace no sirve dos veces.

## 3. Archivos principales

- **Base de datos:** `kinet_bd.sql` (roles, usuarios de prueba, `PLATAFORMA_DEPORTE`, `EVIDENCIA_ENTRENAMIENTO` con enlace). Ahora son 34 tablas.
- **Backend:** `SecurityConfig`, `Rol`, `EnlaceDeporte`, `PlataformaDeporte`, `EvidenciaService`, `EntrenamientoService`, `MovimientoXpRepository`, `EntrenamientoController`, `TareasProgramadas`.
- **Pruebas:** pasan las 85 (`mvn test`). Nuevas: `EnlaceDeporteTest` y casos de enlace, plazo, app apagada, rechazo y competir en `EntrenamientoIntegracionTest`, `CompetitivoIntegracionTest`, `CompetenciaIntegracionTest` y `StaffIntegracionTest`.
- **Documentos:** `backend/README.md`, `backend/kinet.http`, `docs/MIGRACION_V2.md` y los entregables .docx (RN-12, Definición, Plan de Calidad, SAD).

## 4. Pendiente o asumido

- **Rango de 180 a 240 min.** Se pidió "180 a 140"; se tomó 240, el máximo actual.
- **Patrones de enlaces** de cada app: confirmarlos con enlaces reales.
- **Retos y rachas** no exigen evidencia.
- Si el enlace llega después de que cierra el enfrentamiento o la ronda, esa sesión ya no cuenta.

## 5. Recargar la base del Docker

El script borra y recrea todo (se pierden los datos actuales del contenedor):

```bash
docker cp kinet_bd.sql kinet-db:/tmp/
docker exec -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 kinet-db sqlplus system/admin123@localhost/FREEPDB1 "@/tmp/kinet_bd.sql"
```

Ya se recargó: 3 roles, 5 apps en `PLATAFORMA_DEPORTE`, 34 tablas y sin errores.
