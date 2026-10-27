package ar.uade.tpo.chess;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the hexagonal architecture mechanically, so the dependency rules the professor approved cannot erode
 * silently: it reads the source files and checks their imports.
 *
 * <ul>
 *   <li>the core depends on nothing but the JDK, itself, and the ports;</li>
 *   <li>within the core, {@code model} knows nothing of {@code rules} or {@code game}, and {@code rules} knows
 *       nothing of {@code game};</li>
 *   <li>ports depend only on the JDK and the core model;</li>
 *   <li>nothing themed (NBA, Swing, JavaFX) ever appears in the core.</li>
 * </ul>
 */
class ArchitectureTest {

    private static final Path MAIN = Paths.get("src", "main", "java", "ar", "uade", "tpo", "chess");

    private static List<Path> sourcesIn(String... subPackage) throws IOException {
        Path dir = MAIN;
        for (String part : subPackage) {
            dir = dir.resolve(part);
        }
        assertTrue(Files.isDirectory(dir), "source directory not found (run the tests from the project root): " + dir);
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static List<String> importsOf(Path file) throws IOException {
        List<String> imports = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                imports.add(trimmed.substring("import ".length()).replace("static ", "").replace(";", "").trim());
            }
        }
        return imports;
    }

    private static void assertOnlyImports(List<Path> files, String... allowedPrefixes) throws IOException {
        for (Path file : files) {
            for (String imported : importsOf(file)) {
                boolean allowed = false;
                for (String prefix : allowedPrefixes) {
                    allowed |= imported.startsWith(prefix);
                }
                assertTrue(allowed, file.getFileName() + " imports " + imported + ", which its layer must not depend on");
            }
        }
    }

    @Test
    void theCoreDependsOnlyOnTheJdkItselfAndThePorts() throws IOException {
        List<Path> core = sourcesIn("core");
        assertFalse(core.isEmpty());
        assertOnlyImports(core, "java.", "ar.uade.tpo.chess.core.", "ar.uade.tpo.chess.ports.");
    }

    @Test
    void theCoreModelDependsOnNothingAboveIt() throws IOException {
        assertOnlyImports(sourcesIn("core", "model"), "java.");
    }

    @Test
    void theRulesDependOnlyOnTheModel() throws IOException {
        assertOnlyImports(sourcesIn("core", "rules"), "java.", "ar.uade.tpo.chess.core.model.");
    }

    @Test
    void thePortsDependOnlyOnTheJdkAndTheCoreModel() throws IOException {
        List<Path> ports = sourcesIn("ports");
        assertFalse(ports.isEmpty());
        assertOnlyImports(ports, "java.", "ar.uade.tpo.chess.core.model.");
    }

    @Test
    void noThemeOrFrameworkNameAppearsInTheCore() throws IOException {
        for (Path file : sourcesIn("core")) {
            String text = Files.readString(file).toLowerCase(Locale.ROOT);
            for (String forbidden : new String[] {"nba", "swing", "javafx", "franchise"}) {
                assertFalse(text.contains(forbidden), file.getFileName() + " mentions '" + forbidden + "'");
            }
        }
    }

    @Test
    void theCoreHasNoMainMethodAndDoesNoInputOutput() throws IOException {
        for (Path file : sourcesIn("core")) {
            String text = Files.readString(file);
            assertFalse(text.contains("System.out"), file.getFileName() + " prints");
            assertFalse(text.contains("System.in"), file.getFileName() + " reads input");
            assertFalse(text.contains("static void main"), file.getFileName() + " has a main method");
        }
    }

    @Test
    void theMechanicsAndConsoleLiveOnlyInAdapters() throws IOException {
        for (Path file : sourcesIn("adapters")) {
            assertTrue(file.toString().contains("adapters"));
        }
        List<Path> adapters = sourcesIn("adapters");
        assertFalse(adapters.isEmpty());
        for (Path file : adapters) {
            for (String imported : importsOf(file)) {
                assertFalse(imported.startsWith("javax.swing") || imported.startsWith("javafx"),
                        file.getFileName() + " should not need a GUI toolkit");
            }
        }
    }
}
