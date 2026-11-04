# Kinet · Monitoreo y registro de incidentes

Responde a la auditoría del 03/11/2026: criterios 6.10.6 y 6.10.7, riesgo 7.

## 1. Qué se vigila

| Qué | Cómo | Alerta |
|---|---|---|
| Que la app responda | Monitor externo **UptimeRobot** sobre `https://kinetapp.duckdns.org` cada 5 min | Correo al equipo si falla 2 veces seguidas |
| Que la API y la base de datos estén bien | UptimeRobot sobre `https://kinetapp.duckdns.org/api/sistema/health` (palabra clave `UP`) | Igual |
| Certificado HTTPS | UptimeRobot avisa 7 días antes de que venza | Correo |
| Contenedores | `docker compose ps` (con *healthcheck* de la base de datos) | Revisión diaria del responsable de turno |
| Respaldo diario | `~/respaldos/respaldo.log` | El script termina con error si falla |
| Errores de la app | `docker compose logs --since 24h backend \| grep -i error` | Revisión diaria |

`/api/sistema/health` es Spring Boot Actuator. Responde `{"status":"UP"}` si la API funciona **y** llega a Oracle; si la base de datos cae, responde 503 `{"status":"DOWN"}`. No muestra detalles internos y el resto de Actuator no está expuesto.

## 2. Configurar UptimeRobot (gratis)

1. Crear la cuenta en uptimerobot.com con el correo del equipo.
2. **Add New Monitor → HTTP(s)** → `https://kinetapp.duckdns.org` → intervalo 5 minutos.
3. **Add New Monitor → Keyword** → `https://kinetapp.duckdns.org/api/sistema/health` → palabra `UP` → "alert when keyword not exists".
4. En *Alert Contacts*, agregar los correos de los tres integrantes.
5. Crear una *Status Page* pública con los dos monitores. Su enlace es la evidencia del monitoreo.

## 3. Registro de incidentes

Todo incidente (caída, error en producción, dato perdido, fallo del respaldo o problema de seguridad) se registra en Jira, en el proyecto KAN:

- **Tipo:** Tarea con la etiqueta `incidente`.
- **Título:** `[Incidente] DD/MM/AAAA · qué pasó`.
- **Descripción:** fecha y hora de inicio, impacto (quién se vio afectado y cuánto tiempo), causa, solución y acción preventiva.
- **Estado:** *En curso* mientras se atiende y *Finalizado* cuando se resuelve.

Para ver todos los incidentes: filtro `project = KAN AND labels = incidente ORDER BY created DESC`.

Ya registrados: KAN-71 (despliegue bloqueado, 21/10), KAN-72 (identidad de Git, 03/11) y KAN-73 (correos FALLIDA por SMTP sin configurar).
