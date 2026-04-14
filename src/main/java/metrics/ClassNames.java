package metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassNames {

    public static List<String> getJavaClassesName(String repoPath) throws IOException {
        Path projectRoot = Paths.get(repoPath);

        try (Stream<Path> walk = Files.walk(projectRoot)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    // Escludiamo i test ma teniamo tutto il resto della struttura
                    .filter(p -> !p.toString().contains("/test/") && !p.toString().contains("\\test\\"))
                    // TRICK: prendiamo il percorso dalla root del progetto in poi
                    .map(p -> projectRoot.relativize(p).toString())
                    .collect(Collectors.toList());
        }
    }


}
