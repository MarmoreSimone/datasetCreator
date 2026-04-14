package utils;

import entity.ReleaseInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class csvReader {

    public static List<ReleaseInfo> getReleasesInfo(String filePath, double releasePercentage) {
        // filePath: percorso dove si trova il csv generato dal codice (es. OPENJPAVersionInfo.csv)
        // releasePercentage: percentuale di release su cui vogliamo fare il dataset (es. 0.34)

        List<ReleaseInfo> allReleases = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Salta l'header

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                if (values.length >= 4) {

                    allReleases.add(new ReleaseInfo(values[0].trim(), values[2].trim(), values[3].trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file CSV: " + e.getMessage());
        }

        // Prendo solo la percentuale che mi interessa, arrotondando per eccesso
        int limit = (int) Math.ceil(allReleases.size() * releasePercentage);

        if (!allReleases.isEmpty()) {
            System.out.println("File CSV letto con successo. Release selezionate: " + limit +
                    " (cioè il " + (releasePercentage * 100) + "% del totale).");
        }

        return allReleases.subList(0, limit);
    }

}
