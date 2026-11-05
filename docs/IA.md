# Jugar contra la computadora

En Inicio elegí cualquiera de los cuatro modos. En la configuración, cambiá **Rival** a **Computadora · IA local**, elegí dificultad y color, y comenzá la partida.

- **Fácil:** analiza una jugada y varía entre opciones de valor cercano.
- **Media:** analiza hasta dos jugadas de profundidad.
- **Difícil:** analiza hasta tres, con un presupuesto mayor de tiempo y posiciones.

Es un rival local de ajedrez, sin modelos de lenguaje, cuentas, claves ni conexión externa. Evalúa material, posición de las piezas y reglas NBA mediante minimax con poda alfa-beta. El tiempo de búsqueda está limitado; no se presenta como un motor de nivel profesional ni tiene un Elo medido.

El motor Java sigue validando cada acción, incluyendo promoción, enroque, captura al paso, intercambios, puntos y turnos extra. La IA analiza sobre una copia independiente que conserva repetición, en passant, derechos de enroque y extensiones NBA, sin tocar el historial real ni sus observadores.

Deshacer y rehacer pausan la respuesta automática. **Continuar contra IA** retoma la partida; jugar una nueva acción propia también la retoma. Al elegir negras, la IA empieza con blancas y el tablero se gira para vos. Si falla la conexión, aparece un botón de reintento y no se insiste automáticamente.

Contra la computadora, una propuesta de tablas recibe una respuesta automática: el rival acepta si no lleva ventaja material y de puntos; en caso contrario, continúa jugando. En modo local se conservan las confirmaciones de ambos jugadores.

## Adaptador web

`POST /api/new` admite `opponent=local|ai`, `humanTeam=WHITE|BLACK` y `difficulty=EASY|MEDIUM|HARD`. Los valores se validan antes de reemplazar la partida.

`POST /api/ai-move` ejecuta exactamente una acción del rival cuando le corresponde y no está pausado. `POST /api/ai-resume` desactiva la pausa. El front vuelve a solicitar una acción si una regla NBA conserva el turno de la computadora.

La respuesta de estado agrega `opponent`, `aiTeam`, `difficulty`, `aiPaused` y `lastAiMove`. El servidor rechaza acciones humanas durante el turno de la IA.

Validación: pruebas de legalidad en los tres niveles, mate en una, análisis sin alterar historial/rehacer, copia con repetición y reglas NBA, controles HTTP de turno/configuración/pausa, y recorrido del JavaScript real con DOM simulado contra un servidor HTTP.
