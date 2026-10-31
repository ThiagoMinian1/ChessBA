package ar.uade.tpo.chess.adapters.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Punto de entrada para jugar desde stdin/stdout. Ejecutar {@code java -jar chess-1.0.0.jar} después de {@code mvn package}. */
public final class ConsoleApp {

    private ConsoleApp() {
    }

    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        new ConsoleRunner(in, System.out).run();
    }
}
