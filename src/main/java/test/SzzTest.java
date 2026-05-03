package test;

import csv.CsvReader;
import entity.ReleaseInfo;
import entity.TicketBug;
import labeling.Szz;

import java.util.List;

public class SzzTest {

    private static final String BUGGY_TICKET_PATH = "jiraTicketsEnriched.csv";
    private static final String RELEASES_FILE_PATH = "OPENJPAVersionInfo.csv";

    public static void main(){

        List<ReleaseInfo> releases = CsvReader.getReleasesInfo(RELEASES_FILE_PATH, 1);
        List<TicketBug> buggyTicketsList = CsvReader.getTicketsFromCsv(BUGGY_TICKET_PATH,releases);
        Szz.initialMappingSZZ(buggyTicketsList,releases);
        Szz.applyProportion(buggyTicketsList);
        int dimensioneIniziale = buggyTicketsList.size();

        // Rimuoviamo i ticket che violano la logica temporale SZZ:
        // 1. Un bug non può essere risolto prima di essere aperto (FV < OV)
        // 2. Un bug non può essere aperto prima di essere iniettato (OV < IV)
        // Nota: fv < iv è implicitamente coperto dalle due condizioni sopra.
        buggyTicketsList.removeIf(t -> t.getFv() < t.getOv() || t.getOv() < t.getIv());

        int rimossi = dimensioneIniziale - buggyTicketsList.size();

        System.out.println("=== Pulizia Dataset terminata ===");
        System.out.println("Ticket scartati perché inconsistenti: " + rimossi);
        System.out.println("Ticket validi rimanenti: " + buggyTicketsList.size());
        System.out.println("--------------------------------\n");

    }
}
