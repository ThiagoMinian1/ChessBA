# Clases nuevas y relaciones (TPO Ajedrez)

Paquete raíz: `ar.uade.tpo.chess`. Diagramas completos: `docs/uml/chess-classes.puml` (PlantUML) y `docs/uml/chess-classes.mmd` (Mermaid).

## 1. Clases por paquete

**core.model** (solo `java.*`): `Team`, `PieceType`, `Position`, `Move`, `MoveType`, `GameStatus`, `Piece` (abstracta) + `Pawn`, `Rook`, `Knight`, `Bishop`, `Queen`, `King`, `PieceFactory`, `Square`, `PlacedPiece`, `Board`.

**core.rules** (solo model): `MoveRule` (Strategy), `LinearMoveRule` / `StepMoveRule` (Template Method), `Rook/Bishop/Queen/Knight/King/PawnMoveRule`, `MoveRuleFactory`, `MoveContext`, `BasicMoveContext`, `Directions`, `AttackDetector`, `MoveGenerator`, `MoveApplier`, `MoveOutcome`, `MaterialChecker`.

**core.game**: `ChessGame` (interfaz), `Game`, `GameDecorator`, `TurnAction` (Command), `MoveAction`, `ResignAction`, `AgreeDrawAction`, `GameState` (Originator), `GameMemento` (Memento), `MoveRecord`, `GameEvent` (sealed), `IllegalMoveException`, `PromotionRequiredException`.

**ports**: `GameObserver` (Observer), `PieceTheme`.

**adapters**: `StandardPieceTheme`.

**adapters.nba**: `Franchise` (30 equipos), `NbaPieceTheme`, `GameConfig` (+ Builder), `NbaGameFactory`, `ShotClockGame`, `ShotClockAction`, `ShotClockState` (State: Running / Warning / Expired), `ShotClocks`, `ShotClockListener`, `HotHandGame`, `HotHandAction`, `Streaks`, `ScoreboardGame`, `ScoreboardAction`, `Scores`, `TradeAction`, `TradeLedger`.

**adapters.console**: `ConsoleApp`, `ConsoleRunner`, `Command`, `CommandParser`, `ConsoleBoardRenderer`, `ConsoleGameObserver`.

## 2. Patrones

| Patrón | Dónde |
|---|---|
| Strategy + Factory | `MoveRule`, `MoveRuleFactory` |
| Template Method | `LinearMoveRule`, `StepMoveRule` |
| Command | `TurnAction` y sus implementaciones (`MoveAction`, `ResignAction`, `AgreeDrawAction`, `TradeAction`) |
| Memento | `GameMemento`: snapshot antes/después de cada acción; undo/redo restaura copias |
| Decorator | `GameDecorator` → `ShotClockGame`, `HotHandGame`, `ScoreboardGame`; también `ShotClockAction`/`HotHandAction`/`ScoreboardAction` envuelven un `TurnAction` |
| Observer | `GameObserver` (core), `ShotClockListener` (reloj) |
| State | `ShotClockState` (sealed: Running, Warning, Expired) |
| Builder / Factory | `GameConfig.Builder`, `NbaGameFactory`, `PieceFactory` |

## 3. Qué es nuevo respecto del diagrama de capas aprobado

Las capas y sus dependencias no cambian: el core no conoce NBA, ni consola, ni franquicias (lo verifica `ArchitectureTest`). Lo nuevo vive en adapters (`adapters.nba`, `adapters.console`) y en los puertos `GameObserver` / `PieceTheme`.

## 4. Reglas obligatorias del TP que toca cada mecánica

| Mecánica | Regla que toca | Cómo se desactiva |
|---|---|---|
| Shot clock | Alternancia de turnos (puede saltear el turno del equipo con el reloj vencido) | No agregar `ShotClockGame` (`GameConfig` sin `shotClock`) |
| Hot hand | Alternancia de turnos (movida extra tras 2 capturas propias consecutivas) | No agregar `HotHandGame` |
| Scoreboard (tablero de puntos) | Resultado de una partida empatada: las tablas por regla se deciden por puntos | No agregar `ScoreboardGame` (`GameConfig` sin `scoreboard`) |
| Trade | Agrega una acción de tablero que no es un movimiento y consume el turno | No habilitar `trade` en `GameConfig`; la consola lo rechaza |

Con las tres apagadas el juego es ajedrez estándar. Todas son reversibles con undo/redo (el estado de cada decorador está en `GameState.extensions` y viaja en los snapshots).

## 5. Decisiones de diseño a revisar

- **Shot clock**: tras la jugada del actor, si el reloj del rival está Expired se saltea su turno, salvo que esté en jaque o que algún bando quede sin jugadas legales; el salto reinicia su reloj y es parte de la misma entrada del historial. Límite por defecto 10, configurable.
- **Hot hand**: cuenta capturas propias consecutivas (las jugadas rivales no reinician; un trade sí); al llegar a 2 se reinicia y se da jugada extra, salvo que el rival esté en jaque, algún bando no tenga jugadas legales o el turno ya se haya conservado por un salto.
- **Trade**: una vez por equipo; se intercambia una pieza no rey del tablero por una del banco del mismo equipo; no permitido en jaque; un peón no puede entrar a fila 1/8; la pieza entrante cuenta como movida (sin derechos de enroque); reinicia el contador de 50 movimientos, el historial de posiciones y la captura al paso; consume el turno. El banco guarda piezas capturadas y las cambiadas; una pieza promovida capturada va al banco con su tipo promovido.
- **Scoreboard**: cada captura suma el valor de la pieza capturada (peón 1, caballo 3, alfil 3, torre 5, dama 9; una pieza promovida vale como lo que es ahora). Si la partida termina en tablas por ahogado, triple repetición, 50 movimientos o material insuficiente, gana quien tenga más puntos; con puntaje igual sigue siendo tablas. Las tablas por acuerdo nunca se deciden por puntos, y el trade no suma. `status()` sigue informando la tabla real (ej. `DRAW_FIFTY_MOVE_RULE`); solo `winner()` refleja el puntaje, y `ScoreboardGame.decidedOnPoints(game)` distingue ambos casos.
- **Repetición**: identidad de posición = tablero + turno + derechos de enroque + casilla de captura al paso solo si realmente hay captura posible; 3 repeticiones = tablas.
- **50 movimientos**: 100 medios movimientos sin captura ni movimiento de peón.
- **Material insuficiente**: K vs K, K+alfil, K+caballo, y alfiles del mismo color de casilla.
- **Orden de evaluación** en `Game.evaluate`: sin jugadas legales (mate/ahogado) → material insuficiente → repetición → 50 movimientos.
- **Undo/redo**: una acción por vez (una jugada con sus efectos de decoradores es una acción).
