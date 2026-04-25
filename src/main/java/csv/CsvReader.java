package csv;

import entity.ReleaseInfo;

import java.io.*;
import java.util.*;

public class CsvReader {

    private CsvReader(){}

    public static List<ReleaseInfo> getReleasesInfo(String filePath, double releasePercentage) {
        // filePath: percorso dove si trova il csv generato dal codice (es. OPENJPAVersionInfo.csv)
        // releasePercentage: percentuale di release su cui vogliamo fare il dataset (es. 0.34)

        List<ReleaseInfo> allReleases = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            var _ = br.readLine(); // Salta l'header, impiccio con var_ per smell sonarcloud

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

    public static Set<String> retrieveTicketsID(){

        HashSet<String> ticketList = new HashSet<>();

        File file = new File("buggyTicket.txt");

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String riga = scanner.nextLine().trim();
                if (!riga.isEmpty()) {
                    ticketList.add(riga);
                }
            }

        } catch (FileNotFoundException _) {
            System.err.println("Errore: File non trovato!");
        }

        return ticketList;
    }

}
