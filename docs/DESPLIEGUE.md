# Kinet · Despliegue en producción

Cómo está desplegado Kinet y cómo se actualiza. Responde a la auditoría del 03/11/2026 (criterios 6.9.4 y 6.10.2).

## 1. Arquitectura

```
Navegador ──HTTPS──▶ Caddy (puerto 443, certificado Let's Encrypt automático)
                       │  kinetapp.duckdns.org
                       ▼
                 127.0.0.1:8081 ──▶ contenedor frontend (nginx)
                                      ├─ /        → app Angular/Ionic (archivos estáticos)
                                      └─ /api/    → contenedor backend:8080 (Spring Boot)
                                                      └─▶ contenedor db (Oracle Free, volumen persistente)
```

| Pieza | Detalle |
|---|---|
| Servidor | AWS EC2, Ubuntu 26.04 LTS, IP elástica `16.58.42.198` |
| Dominio | `kinetapp.duckdns.org` (DuckDNS, apunta a la IP elástica) |
| HTTPS | Caddy en el host. Pide y renueva el certificado solo. Agrega HSTS, `X-Frame-Options` y `nosniff` |
| Contenedores | Docker Compose en `~/proyecto`: `db`, `backend`, `frontend` |
| Código | `~/proyecto/back` (este repo) y `~/proyecto/front` (kinet-frontend), clonados con *deploy keys* de solo lectura |
| Secretos | Solo en `~/proyecto/.env` (permisos 600). Nunca en el repositorio |
| Puertos abiertos (grupo de seguridad) | 22 (SSH, solo con llave), 80 y 443. La base de datos y el backend no se exponen |

> AWS y DuckDNS no se contradicen: el servidor es una instancia EC2 de AWS y DuckDNS solo da el nombre de dominio gratuito.

## 2. Variables del `.env` del servidor

| Variable | Uso |
|---|---|
| `ORACLE_PASSWORD` | Clave de SYSTEM del contenedor de Oracle (también la usa `ops/respaldo.sh`) |
| `DB_URL`, `DB_USUARIO`, `DB_CONTRASENA` | Conexión del backend como `KINET_APP` |
| `KINET_JWT_SECRET` | Secreto de las sesiones, mínimo 32 caracteres. **Sin él la API no arranca** |
| `KINET_CORS_ORIGENES`, `KINET_FRONTEND_URL` | `https://kinetapp.duckdns.org` |
| `MAIL_*` | Correo saliente (pendiente, KAN-50) |

Generar un secreto nuevo: `openssl rand -base64 48`.

## 3. Primer despliegue (servidor nuevo)

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git caddy
sudo usermod -aG docker ubuntu        # cerrar y abrir la sesión SSH
mkdir -p ~/proyecto && cd ~/proyecto
git clone git@github-back:f0spin4v/kinet-backend.git back
git clone git@github-front:f0spin4v/kinet-frontend.git front
cp back/ops/docker-compose.yml back/ops/init-db.sh .
nano .env && chmod 600 .env           # ver sección 2
sudo cp back/ops/Caddyfile /etc/caddy/Caddyfile && sudo systemctl reload caddy
docker compose up -d --build
```

## 4. Actualizar a una versión nueva

1. Los cambios entran a `main` por *pull request*, con la integración continua en verde.
2. En el servidor:
   ```bash
   cd ~/proyecto/back  && git pull
   cd ~/proyecto/front && git pull
   cd ~/proyecto && docker compose up -d --build
   docker compose ps                      # los tres en "Up" / "healthy"
   curl -s https://kinetapp.duckdns.org/api/sistema/health   # {"status":"UP"}
   ```
3. Si la versión trae cambios de base de datos, se aplica antes su migración de `db/migraciones/` (ver `RESPALDO.md`: primero se respalda).

## 5. Volver a la versión anterior

```bash
cd ~/proyecto/back && git log --oneline -5      # elegir el commit bueno
git checkout <commit> && cd ~/proyecto && docker compose up -d --build backend
```
Si el problema es de datos, se restaura el último respaldo (`RESPALDO.md`, sección 4).

## 6. Ambientes

| Ambiente | Dónde | Datos |
|---|---|---|
| Desarrollo / pruebas | Equipo de cada integrante: Docker local (`kinet-db`) + backend en IntelliJ + `ng serve` | `kinet_bd.sql` (datos de prueba) |
| Pruebas automáticas | GitHub Actions en cada *pull request* (H2 en modo Oracle y Playwright) | Generados por cada prueba |
| Producción | EC2 (este documento) | Datos reales. Sin cuentas de prueba con claves públicas |
