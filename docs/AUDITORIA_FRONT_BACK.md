# Cobertura del front respecto del motor

El front usa las jugadas legales y el estado devueltos por Java. No calcula por su cuenta el ganador, los puntos ni los turnos extra.

| Función del back | Acceso en el front |
| --- | --- |
| Ajedrez clásico, NBA completo, NBA solo estética y reglas combinadas | Cuatro opciones explícitas en Inicio y en configurar partida |
| 30 franquicias | Selectores con búsqueda y logos |
| Movimientos legales y jaque | Casillas marcadas, selección, teclado y arrastre |
| Enroque y captura al paso | Seleccionar origen y destino legal; Java mueve las piezas correspondientes |
| Promoción a dama, torre, alfil o caballo | Diálogo de elección antes de enviar la jugada |
| Shot clock configurable | Límite 1–24, cuentas por equipo, aviso de agotamiento y turno actualizado |
| Hot hand | Rachas por equipo, anuncio y turno extra real |
| Intercambios | Banco, selección de pieza propia y confirmación; una vez por equipo |
| Marcador | Puntos reales, feedback de captura y resumen final |
| Rendición y tablas acordadas | Botones con confirmación |
| Mate, ahogado, repetición, 50 movimientos y material insuficiente | Detección de Java, explicación y pantalla final |
| Contador de medias jugadas | Panel Jugadas, para entender la regla de 50 movimientos |
| Desempate por puntos | Resultado del motor; las tablas acordadas conservan el empate |
| Historial, deshacer y rehacer | Panel Jugadas y controles; también desde el resultado |
| Banco, estado de intercambios y estadísticas finales | Banco, panel NBA y resultado |

Los observadores, decoradores y snapshots son mecanismos internos: sus efectos se presentan mediante estado, historial y resultado. La partida vive en la sesión en memoria; el motor no ofrece guardado en disco ni juego en red entre navegadores.

La comprobación incluye pruebas del motor y del adaptador HTTP, más un recorrido del JavaScript real con DOM simulado contra el servidor. Este último verifica lógica y conexión, no constituye una inspección visual del navegador.
