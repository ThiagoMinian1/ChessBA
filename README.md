# Chess TPO (Java 21 + Maven)

## Interfaz web ChessBA

Desde la raíz del proyecto, iniciá la web con estos comandos. Luego abrí **http://127.0.0.1:8080**:

```sh
mvn compile
java -cp target/classes ar.uade.tpo.chess.adapters.web.WebApp
```

Para usar otro puerto, agregalo al final del comando Java, por ejemplo `8081`.
El comando original `java -jar target/chess-1.0.0.jar` sigue abriendo la consola.

Dejá la terminal abierta y usá `Ctrl+C` para detener el servidor. Si el puerto está
ocupado, agregá `8081` al comando Java y abrí `http://127.0.0.1:8081`.
Después de modificar los recursos de la web, ejecutá `mvn compile` y recargá el navegador
con `Ctrl+F5`. Si ejecutás desde `target/classes`, los cambios de recursos no requieren
reiniciar el servidor. Los cambios de código Java sí requieren reiniciarlo.

La web permite jugar entre dos personas en el mismo dispositivo: seleccioná o arrastrá
una pieza a un destino legal. Incluye promoción, historial, deshacer/rehacer, giro de
tablero, sonido opcional, tablas de común acuerdo y rendición. Al crear una partida
podés elegir ajedrez clásico, NBA completo o configurar sus mecánicas y franquicias.
Para intercambiar, seleccioná una pieza del tablero y luego una del banco propio.
El estilo visual usa negro, blanco y naranja; las piezas clásicas son la opción inicial
y el botón Casacas permite vestir las piezas conservando sus siluetas de ajedrez. El tablero se
puede recorrer con las flechas del teclado y jugar con Enter o espacio. Escape cancela
la selección. El sonido y el estilo de piezas se recuerdan en el navegador.

La cancha usa una proporción vertical de 5:8. Cada captura muestra una animación con su valor y
combina un golpe grave con el swish. En el panel **Tribuna** se pueden probar y activar
palmas, "Defense" o "Let's go". Los cantos usan la síntesis de voz del navegador;
las palmas y los golpes se generan con Web Audio. Con la tribuna activada suenan
ocasionalmente durante la partida. El botón general de sonido silencia también la tribuna.

### Menú, marcador y resultado

Al abrir la web se muestra el menú inicial: NBA completo, clásico o personalizado.
Elegir un modo abre la configuración; el tablero aparece recién al iniciar la partida.
El menú permite continuar la partida de la sesión o revisar su resultado sin reiniciarla.

La integración de `chess (1).zip` agrega `Scores`, `ScoreboardAction` y `ScoreboardGame`,
la configuración y fábrica actualizadas, la consola, sus pruebas y los diagramas nuevos.
En NBA completo se activan las cuatro mecánicas. En personalizado se puede elegir el
marcador por separado. Capturar un peón suma 1, caballo o alfil 3, torre 5 y dama 9;
la animación muestra ese valor cuando el marcador está habilitado.

El resultado y los puntos vienen del backend. Las tablas por reglas se deciden por
puntos si el marcador está activo y hay diferencia; las tablas acordadas siempre se
respetan. Mate y rendición conservan su ganador. Deshacer/rehacer restaura los puntos
y el resultado. La pantalla final muestra ganador, motivo, marcador y piezas restantes,
y permite revisar el tablero, deshacer el final o iniciar otra partida.

`GET /api/result` devuelve el resultado final, o HTTP 409 si la partida sigue en curso.
`GET /api/state` incluye `started`, `scoreboard`, `whiteScore`, `blackScore`,
`decidedOnPoints` y `result` (null mientras se juega). La sesión y su resultado están
en memoria; no se agregó un historial persistente de varias partidas.

Los escudos se cargan desde el CDN de NBA (`cdn.nba.com`), usando los identificadores de
las [franquicias](https://www.nba.com/teams). Aparecen en el selector con búsqueda, el
enfrentamiento, las mecánicas, las casacas y el centro de la cancha. Para verlos hace
falta conexión a internet; si una imagen no carga, los controles mantienen el nombre
del equipo y las siglas como respaldo. Las marcas pertenecen a sus respectivos titulares.
El parquet y las líneas son gráficos propios, inspirados en la referencia de cancha.

El adaptador `adapters.web.WebApp` sirve la interfaz y una API HTTP con el JDK,
sin dependencias adicionales. Las reglas se ejecutan en `ChessGame` y los decoradores
existentes. Las partidas se conservan en memoria por cookie de navegador, hasta detener
el servidor o alcanzar 128 sesiones (se descarta la más antigua). No incluye usuarios,
persistencia ni multijugador remoto. El servidor escucha solo en `127.0.0.1`.

API: `GET /api/state`, `GET /api/franchises`; acciones `POST /api/new`, `/api/move`,
`/api/undo`, `/api/redo`, `/api/trade`, `/api/draw`, `/api/resign` con campos de formulario.
El estado JSON incluye tablero, jugadas legales y estado de las mecánicas.

Backend of a chess game with hexagonal architecture (`core` / `ports` / `adapters`).
The core is pure standard chess; the four optional NBA mechanics (shot clock, trade, hot hand, scoreboard) live in `adapters.nba`.

## Build and run

```
mvn test                              # runs the whole JUnit 5 suite
mvn package                           # builds target/chess-1.0.0.jar
java -jar target/chess-1.0.0.jar      # console game
```

Console modes: 1 standard chess, 2 NBA with every mechanic, 3 NBA custom, 4 NBA theme only.
Type `help` in the game for the commands (`e2 e4`, `undo`, `redo`, `trade e4 q`, `bench`, `resign`, `draw`, `quit`).

## Layout

- `src/main/java/ar/uade/tpo/chess/core/{model,rules,game}`: standard chess, no external dependencies.
- `.../ports`: `GameObserver`, `PieceTheme`.
- `.../adapters`, `adapters/nba`, `adapters/console`: themes, mechanics, console UI.
- `docs/uml`: class diagrams (`.puml` PlantUML, `.mmd` Mermaid); `docs/CLASES_Y_RELACIONES.md`: classes, patterns, decisions.
