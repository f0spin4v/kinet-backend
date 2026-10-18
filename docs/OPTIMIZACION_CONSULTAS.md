# Kinet: optimización de consultas en los servicios

Se revisaron los 20 servicios del backend buscando trabajo que hacía Java y que la base de datos puede hacer mejor:
- traer filas para filtrarlas, contarlas o sumarlas en memoria;
- consultar dentro de un bucle (el problema "N+1");
- cargar entidades completas para usar uno o dos campos;
- actualizar fila por fila lo que se puede actualizar en bloque.

Todo eso pasó a consultas JPQL en los repositorios.

## 1. Cómo se midió

[`ConsultasPorPeticionTest`](../backend/src/test/java/co/edu/uniquindio/kinet/ConsultasPorPeticionTest.java) cuenta las sentencias SQL de cada endpoint con las estadísticas de Hibernate. Usa un escenario con:
- 8 jugadores;
- un clan de 6 miembros;
- una competencia de 7 participantes;
- colocaciones terminadas y entrenamientos;
- retos inscritos.

| Endpoint | Antes | Ahora | Qué lo causaba |
|---|---:|---:|---|
| `GET /api/soporte/usuarios` | 21 | 2 | Perfil, rol y nivel consultados por cada usuario; filtro de estado en Java |
| `GET /api/temporadas/activa/ranking` | 11 | 2 | Un perfil por jugador para el gamertag; la temporada completa se cargaba y se cortaba en Java |
| `GET /api/clanes/{id}` | 10 | 3 | Un perfil por miembro; líder buscado recorriendo los miembros |
| `GET /api/retos` | 16 | 7 | Todos los retos activos filtrados en Java y una consulta de participación por reto |
| `GET /api/retos/mios` | 10 | 4 | El reto de cada participación se cargaba aparte |
| `GET /api/competencias` y `/invitaciones` | 14 | 7 | Filtros en Java, participantes y gamertags por competencia, XP en vivo por jugador |
| `GET /api/competencias/{id}` | 13 | 6 | Gamertag y XP en vivo por participante |
| `GET /api/logros` | 7 | 2 | Se cargaban todos los logros obtenidos y luego cada logro |
| `GET /api/catalogos/actividades` | 6 | 2 | Todas las reglas históricas filtradas en Java, más la actividad de cada regla |
| `GET /api/clanes` | 4 | 1 | Conteo de miembros, líder y gamertag por cada clan |
| `GET /api/temporadas/activa/mia` | 7 | 4 | Perfil, rol y rangos en consultas separadas |
| `GET /api/matchmaking/enfrentamientos` | 7 | 5 | Gamertag y XP en vivo por participante |
| `GET /api/progreso/movimientos` | 4 | 2 | Se cargaban las entidades para armar el DTO |
| `POST /api/matchmaking/solicitudes` | 12 | 9 | Por cada jugador en cola: ¿tiene enfrentamiento en curso? y su rango |
| `POST /api/entrenamientos/en-curso/fin` | 65 | 34 | Ver abajo |
| `POST /api/clanes/mio/salida` | 8 | 5 | Retos del clan recorridos uno por uno |

**Por qué importa:** las cifras de "Antes" crecen con los datos y las de "Ahora" no. Con 500 jugadores, el ranking pasaba de 11 a 503 consultas; ahora sigue en 2.

La prueba deja esos números como **topes**. Si alguien vuelve a consultar fila por fila, la prueba falla.

**Finalizar un entrenamiento (65 → 34).** Antes:
- la tabla de niveles se consultaba 8 veces;
- el reto de cada participación se cargaba por separado;
- se revisaba un aporte repetido por cada participación;
- las 8 métricas de logros eran 8 consultas;
- cada notificación costaba 3 sentencias.

Lo que queda son escrituras necesarias: el movimiento de XP, los aportes, las participaciones, el perfil y las notificaciones.

## 2. Técnicas usadas

| Técnica | Dónde |
|---|---|
| **Cargar el usuario con su rol y su perfil** (`@EntityGraph`). `Usuario.perfil` es el lado inverso de un 1:1, así que Hibernate no puede hacerlo perezoso: hacía 3 consultas por usuario. | `UsuarioRepository.findByCorreo`, `findById`, `buscar`, `conRolYPerfil` |
| **Gamertags en lote:** una consulta `IN` en vez de una por fila (`UsuarioService.gamertags`) | Ranking, pendientes de moderación, matchmaking, competencias |
| **Proyecciones** (interfaces o `SELECT new ...DTO`): solo las columnas que usa la respuesta | Ranking, movimientos, miembros del clan, resumen de clanes, catálogo de logros, XP por entrenamiento |
| **Subconsultas en el `SELECT`:** varios conteos en una fila | Resumen de clanes (miembros y líder), métricas de logros (8 en 1) |
| **`COUNT` / `SUM` / `GROUP BY` en la base** | Topes diarios del anti-trampa, XP por jugador en una ventana (enfrentamientos y competencias), posición en la liga para ascenso y descenso |
| **Filtros en el `WHERE`** en vez de `stream().filter()` | Retos visibles (fechas, temporada, nivel), invitaciones y competencias, búsqueda de soporte, rival de matchmaking |
| **`NOT EXISTS`** | Logros pendientes; participaciones donde cuenta un entrenamiento que todavía no aportó |
| **`JOIN FETCH`** | Enfrentamientos con temporada y participantes; competencias con participantes; reglas con su actividad |
| **`UPDATE` en bloque** (`@Modifying`) | Vencer retos, finalizar un reto, cerrar o abandonar un clan, cerrar la temporada (enfrentamientos, cola, puntos de quien abandonó), salir de la cola, invalidar enlaces de recuperación, estado de envío de la notificación |
| **Caché** (`@Cacheable` en el repositorio, `@CacheEvict` al guardar) | Tabla de niveles de progresión: se lee en cada movimiento de XP y solo cambia cuando el administrador la edita |

**Reglas que se movieron al SQL** (el comportamiento es el mismo):
- **Rival de matchmaking:**
  ```sql
  ORDER BY ABS(orden_rango - :mio), fecha_solicitud
  ```
  Se queda con el primero (`FETCH FIRST 1`). Solo considera participaciones ACTIVA sin enfrentamiento en curso.
- **Aportes a retos:** el entrenamiento cuenta si se cumple todo esto:
  - empezó dentro del periodo;
  - terminó después de la inscripción;
  - la métrica no es REPETICIONES;
  - es de la actividad pedida (si el reto pide una);
  - todavía no aportó.
- **Ascenso y descenso:** la posición en la liga es la cantidad de jugadores que quedaron por delante: más puntos finales o, con los mismos puntos, inscripción anterior.

## 3. Lo que se dejó en Java, y por qué

- **Posición final al cerrar la temporada:** cada fila recibe un número distinto y un aviso. Los que abandonaron sí se actualizan en bloque.
- **Cronómetros abandonados:** la duración es distinta para cada fila y cada jugador recibe un aviso.
- **Notificaciones:** se guardan PENDIENTE y luego se marcan ENVIADA o FALLIDA, para no perder avisos si el proceso se cae a mitad. La segunda escritura ahora es un `UPDATE` directo (antes era leer y luego guardar).
- **Temporada activa:** se consulta una vez por movimiento de XP. Es una consulta indexada y cambia al activar o cerrar temporadas; ponerla en caché obligaría a invalidarla en varios lugares.
- **Reglas de puntuación al validar un entrenamiento:** ya era una consulta puntual.

## 4. Detalles técnicos

- **`LIKE` en H2:** en modo Oracle, H2 convierte `''` en NULL. Hibernate agrega `escape ''` a los `LIKE`, así que nunca coincidían. Por eso las consultas declaran `ESCAPE '\'` (en el text block de Java: `'\\'`).
- **Parámetros opcionales:** H2 no acepta un parámetro enum nulo en `:estado IS NULL OR ...`. Se usan dos consultas (`UsuarioRepository.buscar`) o un valor imposible (`idNivel = -1` en los retos).
- **`UPDATE` en bloque:** usan `flushAutomatically = true` (guardan antes los cambios pendientes) pero no limpian el contexto. Por eso se ejecutan antes de cargar esas filas en la misma transacción, como en `vencerDe` al principio de cada consulta de retos.
- **Métodos eliminados:** los que quedaron sin uso, como `countByUsuarioIdAndEstadoValidacion`, `sumarMinutos`, `findTop100...`, `findByEntrenamientoIdIn`, `rankingFinalDeLiga`, `deUsuarioEnEstado` y los de cola y enfrentamientos por temporada.
- **Proyecciones reutilizables:** `IdTexto` (id, texto) e `IdValor` (id, número).
- **Paginación:** las listas que crecen sin tope devuelven un `PaginaDTO` (`contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`) y reciben `?pagina=0&tamanio=20` (tamaño máximo 100, `PaginaDTO.solicitar` acota los valores). Son: usuarios de soporte, clanes, competencias (mías e invitaciones), historial de entrenamientos, pendientes y evidencias de moderación, historial de evaluaciones, enfrentamientos, notificaciones, movimientos de XP, retos (disponibles, míos y admin), temporadas e historial de rango. Los catálogos pequeños y el ranking (que ya tiene `limite`) siguen igual. Cada consulta fija su orden, con el id como desempate.
- **Paginar con `JOIN FETCH` de colecciones:** Hibernate pagina en memoria, así que competencias y enfrentamientos primero piden la página de ids (`idsDeUsuario`) y luego cargan esas filas con sus participantes (`conParticipantes`): una consulta más. Spring Data omite el `COUNT` si la página no viene llena.

## 5. Verificación

- `mvn test`: pasan las **73 pruebas**, incluida `ConsultasPorPeticionTest` con sus topes.
- **Oracle Free (Docker):**
  - el backend arrancó como `kinet_app` con `ddl-auto=validate`;
  - se recorrieron todos los endpoints de la tabla, más aprobar una sesión pendiente, ajustar XP, recuperar contraseña, salir de un clan y cerrarlo, finalizar un reto, abandonar la temporada y cerrarla;
  - todo respondió 2xx, sin errores ORA- en el log;
  - las actualizaciones en bloque dejaron los datos esperados. Por ejemplo, al cerrar la temporada, quien abandonó conservó sus puntos sin posición.
- Al terminar se recargó `kinet_bd.sql` para restaurar los datos de prueba.
