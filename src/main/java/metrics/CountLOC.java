package metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CountLOC {

    public static int countLoc(String repoPath, String relativeFilePath) {
        // Costruiamo il percorso completo del file
        File file = new File(repoPath, relativeFilePath);
        int loc = 0;

        // Se il file non esiste (magari è stato rinominato o cancellato in questa release), torniamo 0
        if (!file.exists()) return 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = reader.readLine()) != null) loc++;

        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file per contare le LOC: " + file.getPath());
        }

        return loc;
    }

}
