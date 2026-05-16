package csv;

import entity.ReleaseInfo;
import entity.TicketBug;

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
            var _ = br.readLine(); // salta l'header, impiccio con var_ per smell sonarcloud

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                if (values.length >= 4) {

                    allReleases.add(new ReleaseInfo(values[0].trim(), values[2].trim(), values[3].trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("errore durante la lettura del file CSV delle release: " + e.getMessage());
        }

        // prendo solo la percentuale che mi interessa, arrotondando per eccesso
        int limit = (int) Math.ceil(allReleases.size() * releasePercentage);

        if (!allReleases.isEmpty()) {
           System.out.println("CSV delle release letto con successo. Release selezionate: " + limit +" (cioè il " + (releasePercentage * 100) + "% del totale).");
        }

        return allReleases.subList(0, limit);
    }

    public static List<TicketBug> getTicketsFromCsv(String filePath, List<ReleaseInfo> releases) {
        List<TicketBug> tickets = new ArrayList<>();
        int c = 0;
        int scartati = 0;

        // lista delle release che consideriamo per szz
        Set<String> officialReleaseNames = new HashSet<>();
        for (ReleaseInfo rel : releases) {
            officialReleaseNames.add(rel.getReleaseID());
        }

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String header = br.readLine();
            if (header == null) {
                return tickets;
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",", -1);
                c++;

                if (columns.length < 4) {
                    continue;
                }

                String key = columns[0];
                String creation = columns[1];
                String resolution = columns[2];
                String versions = columns[3];

                // il costruttore di TicketBug si occupa di separare le versioni in caso ce ne sia piu di una
                TicketBug ticket = new TicketBug(key, creation, resolution, versions);
                List<String> avList = ticket.getAffectedVersions();

                boolean isAvEmpty = (avList == null || avList.isEmpty());

                // se la lista è vuota (caso in cui non ho av)
                // oppure prendo la prima (la più vecchia) e vedo se la versione esiste tra quelle prese da jira o dai TAG la aggiungo
                if (isAvEmpty || officialReleaseNames.contains(avList.get(0))) {
                    tickets.add(ticket);
                } else {
                    // caso in cui non esiste (fantasma), scarto il ticket
                    scartati++;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella lettura dei ticket: " + e.getMessage());
        }

        System.out.println("----RECUPERO TICKET JIRA----");
        System.out.println("ticket disponibili: " + c);
        System.out.println("ticket scartati perche' aventi prima av non esistente in jira o nei tag: " + scartati);
        System.out.println("");

        return tickets;
    }

}
