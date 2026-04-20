package metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

//classe che si occupa di contare le righe di codice di ogni classe, consideriamo anche le righe vuote
public class CountLOC {

    private CountLOC() {
        /* This utility class should not be instantiated */
    }

    public static int countLoc(String repoPath, String relativeFilePath) {
        File file = new File(repoPath, relativeFilePath);

        if (!file.exists()) return 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            // lines() crea uno stream di righe, count() le conta.
            // Poiché count() restituisce un 'long', facciamo un cast a (int)
            return (int) reader.lines().count();

        } catch (IOException _) {
            System.err.println("Errore durante la lettura del file per contare le LOC: " + file.getPath());
            return 0; // Se c'è un errore, restituiamo 0
        }
    }

}
