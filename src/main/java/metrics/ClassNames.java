package metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

//classe che recupera tutti i file che terminano con .java, escludiamo le classi di test
public class ClassNames {

    //costruttore privato per evitare di istanziare la classe
    private ClassNames(){}

    public static List<String> getJavaClassesName(String repoPath) throws IOException {
        Path projectRoot = Paths.get(repoPath);

        try (Stream<Path> walk = Files.walk(projectRoot)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    // Escludiamo i test ma teniamo tutto il resto della struttura
                    .filter(p -> !p.toString().contains("/test/") && !p.toString().contains("\\test\\"))
                    // prendiamo il percorso dalla root del progetto in poi
                    .map(p -> convertPath(projectRoot.relativize(p).toString()))//mantengo solo la parte relativa al file del progetto
                    .toList();
        }
    }

    //USATO PER WINDOWS
    private static String convertPath(String filePath) {

        String convertedPath = filePath.replace("\\", "/");
        if (convertedPath.startsWith("/")) {
            convertedPath = convertedPath.substring(1);
        }

        return convertedPath;

    }
}
