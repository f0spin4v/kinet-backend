# Kinet · Frontend

Aplicación web de Kinet, hecha en Angular 20. Es la interfaz del jugador: se conecta con el backend Spring Boot para registrarse, iniciar sesión y usar la plataforma.

Proyecto de Ingeniería de Software III, Universidad del Quindío.

## Qué incluye hoy

- **Registro** (`/registro`): crea la cuenta con validación en cada campo.
- **Inicio de sesión** (`/login`).
- **Inicio** (`/inicio`): pantalla de llegada con los datos del jugador. Solo se puede ver con sesión iniciada.
- **Manejo de sesión**: guarda el token, lo envía en cada petición y cierra la sesión cuando vence.

## Requisitos

- Node.js 20.19 o superior (revisar con `node -v`).
- El backend de Kinet encendido en `http://localhost:8080`.
- La base de datos Oracle corriendo en Docker (contenedor `kinet-db`).

## Cómo ejecutarlo

Desde la carpeta `frontend`:

```bash
npm install   # solo la primera vez
npm start
```

Luego abrir `http://localhost:4200` en el navegador. La página se recarga sola cada vez que se guarda un cambio en el código.

Para apagarlo: `Ctrl + C` en la terminal.

## Estructura

```
src/
├── index.html            Página base
├── styles.css            Tema visual (colores, botones, formularios)
└── app/
    ├── app.ts            Componente raíz
    ├── app.config.ts     Configuración general
    ├── app.routes.ts     Rutas de la aplicación
    ├── core/             Piezas compartidas
    │   ├── api.ts              Dirección del backend y lectura de errores
    │   ├── auth.service.ts     Registro, login y sesión
    │   ├── auth.interceptor.ts Agrega el token a cada petición
    │   ├── auth.guard.ts       Protege las rutas privadas
    │   ├── modelos.ts          Formas de los datos
    │   └── marca.ts            Logo de Kinet
    └── paginas/
        ├── login/
        ├── registro/
        └── inicio/
```

## Conexión con el backend

La dirección del backend está en `src/app/core/api.ts`:

```ts
export const API_URL = 'http://localhost:8080/api';
```

Ese es el único lugar que hay que cambiar cuando la aplicación se despliegue en AWS.

| Acción | Método y ruta |
|---|---|
| Registrarse | `POST /api/auth/registro` |
| Iniciar sesión | `POST /api/auth/login` |
| Consultar el usuario actual | `GET /api/usuarios/me` |

## Otros comandos

```bash
npm run build   # genera la versión para publicar, en la carpeta dist/
npm test        # ejecuta las pruebas unitarias
```

## Convenciones del equipo

- Nombres de archivos, variables y textos en español.
- Cada pantalla nueva va en su propia carpeta dentro de `paginas/`.
- Los colores y estilos comunes se toman de las variables de `styles.css`, no se repiten en cada pantalla.
- Todo cambio entra por una rama y un pull request, nunca directo a `main`.
