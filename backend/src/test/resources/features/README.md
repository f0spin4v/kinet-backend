# Suite de Pruebas Funcionales BDD - Kinet

Esta carpeta contiene la suite de pruebas funcionales automatizadas del backend de Kinet. Las pruebas siguen la metodología **BDD (Behavior-Driven Development)**: validan los flujos de negocio desde la perspectiva del usuario.

## 🚀 Cómo Ejecutar

### Prerrequisitos
1. **Java 17 o superior** instalado (`java -version`).
2. Nada más. **No** hace falta Oracle, ni tener el backend encendido, ni instalar Maven:
   - La aplicación se levanta sola en memoria con **H2 en modo Oracle**.
   - Maven se descarga solo la primera vez con el **Maven Wrapper** (`backend\mvnw.cmd`).

### Comandos
Desde la carpeta `kinet-backend`:

```powershell
# Solo los escenarios principales (@principal), con su paso a paso
.\pruebas-funcionales.cmd

# Todas las pruebas: todos los escenarios BDD + las pruebas JUnit
.\todas-las-pruebas.cmd
```

Para marcar otro escenario como principal, pon `@principal` en la línea de arriba de su `Escenario`.

**Reporte:** `backend\target\cucumber\reporte.html` (ábrelo en el navegador).

---

## 🧠 Metodología BDD
**Behavior-Driven Development** es una técnica de desarrollo ágil que fomenta la colaboración entre desarrolladores, QA y perfiles no técnicos.

- **Lenguaje Natural (Gherkin)**: los escenarios están escritos en español con las palabras clave `Dado`, `Cuando`, `Entonces` e `Y`.
- **Enfoque en el usuario**: no se prueban métodos del código, sino comportamientos del sistema (por ejemplo: "un jugador registra un entrenamiento y gana XP").
- **Documentación Viva**: los archivos `.feature` son a la vez la especificación y la prueba ejecutable.

---

## 🛠️ Herramientas Utilizadas
- **Cucumber**: el framework de BDD para Java (lee los `.feature` en Gherkin).
- **JUnit 5 Platform Suite**: ejecuta Cucumber dentro de `mvn test`.
- **Spring Boot Test + MockMvc**: levanta la aplicación completa y le hace peticiones HTTP a la API.
- **H2**: base de datos en memoria en modo Oracle, cargada con `data.sql`.

---

## 📂 Estructura

| Archivo | Equivalente en behave (Python) | Qué hace |
|---|---|---|
| `src/test/resources/features/*.feature` | `features/*.feature` | Escenarios en Gherkin. |
| `src/test/java/.../funcionales/steps/Pasos.java` | `features/steps/steps.py` | Pasos: traducen cada frase a peticiones a la API. |
| `src/test/java/.../funcionales/Entorno.java` | `features/environment.py` | Hooks: antes de cada escenario deja los datos en blanco; después, el reloj en la hora real. |
| `src/test/resources/application.properties` | `.env` | Configuración: base H2, clave JWT de pruebas, etc. |
| `src/test/java/.../funcionales/EjecutarPruebasFuncionalesTest.java` | `behave.ini` | Dónde están los features, los pasos y el reporte. |
| `kinet-backend/pruebas-funcionales.cmd` | `run_all.py` | Comando que ejecuta los escenarios `@principal`. |
| `kinet-backend/todas-las-pruebas.cmd` | — | Comando que ejecuta todas las pruebas (BDD + JUnit). |

---

## 📋 Flujos Cubiertos

| Proceso | Archivo | Escenario | ¿Principal? |
|---|---|---|---|
| Humo | `humo.feature` | La aplicación arranca y, sin sesión, la API responde 401. | ✅ |
| Seguridad (RBAC) | `seguridad.feature` | Un jugador no puede entrar a la administración (403). | ✅ |
| Onboarding | `onboarding.feature` | Registro de un jugador nuevo: 201, token de acceso y sin nivel de habilidad. | ✅ |
| Onboarding | `onboarding.feature` | Evaluación de nivel inicial: el jugador queda con el nivel elegido. | ✅ |
| Offboarding | `offboarding.feature` | El administrador bloquea una cuenta y el jugador ya no puede iniciar sesión (403). | ✅ |
| Perfiles | `perfiles.feature` | El jugador actualiza sus nombres y gamertag, y el cambio se guarda. | ✅ |
| Entrenamientos | `entrenamientos.feature` | Un entrenamiento válido queda VALIDADO y otorga XP según la regla de puntuación. | ✅ |

Los casos de detalle (validaciones, errores, límites) están en las pruebas JUnit de `src/test/java/co/edu/uniquindio/kinet/`.

---

## ✍️ Cómo agregar un escenario
1. Escribe el escenario en el `.feature` que corresponda (o crea uno nuevo con `# language: es` en la primera línea).
2. Si usa una frase nueva, agrega el paso en `Pasos.java` con `@Dado`, `@Cuando` o `@Entonces`. Los valores entre comillas se reciben como `{string}` y los números como `{int}`.
3. Ejecuta `.\pruebas-funcionales.cmd 1`. Si falta un paso, Cucumber muestra el código que hay que pegar.
