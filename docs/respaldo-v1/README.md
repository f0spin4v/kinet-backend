# Respaldo del código v1

Código del backend y scripts de base de datos del modelo v1 (11 tablas), retirados al migrar al modelo v2. No se compilan: están aquí solo como referencia.

| Carpeta | Contenido |
|---|---|
| `main/` | Controladores, servicios (interfaces e `impl`), modelo y enums, DTOs, mappers de MapStruct y repositorios de v1 |
| `test/` | Pruebas de integración de v1 y su `data.sql` |
| `db/` | Scripts v1: `usuarios.sql` (proceso 1) y `02_entrenamientos_retos.sql` (proceso 2) |

Qué cambió y por qué: [`../MIGRACION_V2.md`](../MIGRACION_V2.md).
