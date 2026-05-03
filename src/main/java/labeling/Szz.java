package labeling;

import entity.ClassMetrics;
import entity.ReleaseInfo;
import entity.TicketBug;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Szz {

    private Szz(){}

    public static void completeSzz(List<TicketBug> tickets, List<ReleaseInfo> releases){
        initialMappingSZZ(tickets,releases);
        applyProportion(tickets);
        tickets.removeIf(t -> t.getFv() < t.getOv() || t.getOv() < t.getIv());
    }

    // uso i dati presenti nei ticket per trovare iv,ov,fv ove possibile
    public static void initialMappingSZZ(List<TicketBug> tickets, List<ReleaseInfo> releases) {

        int scartatiFvMinoreOv = 0;
        int scartatiOvMinoreIv = 0;

        // uso l'Iterator per poter scartare i ticket durante il ciclo
        Iterator<TicketBug> iterator = tickets.iterator();

        while (iterator.hasNext()) {
            TicketBug ticket = iterator.next();

            // calcolo OV e FV
            calculateOvAndFv(ticket, releases);

            // se fv < ov il ticket è sporco
            if (ticket.getFv() < ticket.getOv()) {
                scartatiFvMinoreOv++;
                iterator.remove();
                continue;
            }

            // calcolo IV
            calculateIv(ticket, releases);

            // se ov < iv il ticket è sporco
            if (ticket.getIv() != -1 && ticket.getOv() < ticket.getIv()) {
                scartatiOvMinoreIv++;
                iterator.remove();
            }
        }

        System.out.println("----INITIAL MAPPING SZZ----");
        System.out.println("Ticket scartati per FV < OV: " + scartatiFvMinoreOv);
        System.out.println("Ticket scartati per OV < IV: " + scartatiOvMinoreIv);
        System.out.println("Ticket rimasti puliti: " + tickets.size());
        System.out.println("");
    }

    private static void calculateOvAndFv(TicketBug ticket, List<ReleaseInfo> releases) {
        if (ticket.getCreationDate() != null) {
            int ov = getReleaseIndexAfterDate(ticket.getCreationDate(), releases);
            ticket.setOv(ov);
        }

        if (ticket.getResolutionDate() != null) {
            int fv = getReleaseIndexAfterDate(ticket.getResolutionDate(), releases);
            ticket.setFv(fv);
        }
    }

    private static void calculateIv(TicketBug ticket, List<ReleaseInfo> releases) {
        List<String> affectedVersions = ticket.getAffectedVersions();

        // caso con Affected Versions: estraggo la prima e calcolo l'indice
        if (affectedVersions != null && !affectedVersions.isEmpty()) {
            String oldestVersion = affectedVersions.get(0);
            int iv = getReleaseIndexByName(oldestVersion, releases);
            ticket.setIv(iv);
            return;
        }

        LocalDate dataRelease2 = LocalDate.parse(releases.get(1).getDate().substring(0, 10));

        // Se il ticket è stato aperto prima dell'uscita della Release 2, il bug è della release 1
        if (ticket.getCreationDate() != null && ticket.getCreationDate().isBefore(dataRelease2)) {
            ticket.setIv(1);
            return;
        }

        // metto iv=-1 e sará calcolato in seguito con proportion
        ticket.setIv(-1);
    }

    // calcola il valore di proportion
    public static double computeProportion(List<TicketBug> tickets){

        List<Double> pValues = new ArrayList<>();
        int uguali=0;
        int senzaav = 0;

        for (TicketBug t : tickets) {
            double iv = t.getIv();
            double ov = t.getOv();
            double fv = t.getFv();

            // prendo solo i ticket con una iv
            if (iv != -1) {
                // per evitare la divisione per zero
                if (fv == ov) {
                    uguali++;
                    continue;
                }

                // Calcolo di P per il singolo ticket
                double p = (fv - iv) / (fv - ov);
                pValues.add(p);

            } else senzaav++;

        }

        // calcolo p medio
        double sum = 0;
        for (Double p : pValues) {
            sum += p;
        }

        double pAvg = sum / pValues.size();
        System.out.println("----CALCOLO DI PROPORTION----");
        System.out.println("ticket con fv == ov: " + uguali);
        System.out.println("ticket senza affected versions: " + senzaav);
        System.out.println("p calcolato su " + pValues.size() + " ticket: " + pAvg);
        System.out.println("");

        return pAvg;
    }

    // uso il valore di proportion per calcolare la iv dei ticket che non c'é l'hanno
    public static void applyProportion(List<TicketBug> tickets) {

        double pAvg = computeProportion(tickets);
        int aggiustamenti = 0;
        int releaseZero = 0;
        int contatoreStime = 0;

        for (TicketBug t : tickets) {
            // prendo i ticket senza iv
            if (t.getIv() == -1) {
                double ov = t.getOv();
                double fv = t.getFv();

                // formula per predirre iv
                double estimatedIvDouble = fv - (fv - ov) * pAvg;

                // arrotondo
                int estimatedIv = (int) Math.round(estimatedIvDouble);

                // Il bug non può essere stato iniettato prima della prima release
                if (estimatedIv < 1) {
                    releaseZero++;
                    estimatedIv = 1;
                }
                // Il bug non può essere stato iniettato DOPO essere stato scoperto (OV)
                if (estimatedIv > ov) {
                    aggiustamenti++;
                    estimatedIv = (int) ov;
                }

                // Salviamo la stima nel ticket
                t.setIv(estimatedIv);
                contatoreStime++;
            }
        }

        System.out.println("----APPLICAZIONE PROPORTION----");
        System.out.println("Stimate le Injected Version per " + contatoreStime + " ticket senza iv");
        System.out.println("ticket a cui é uscita iv<1: " + releaseZero );
        System.out.println("ticket non consistenti: " + aggiustamenti);
        System.out.println("");

    }

    public static int getReleaseIndexAfterDate(LocalDate ticketDate, List<ReleaseInfo> releases) {
        for (ReleaseInfo rel : releases) {
            LocalDate releaseDate = LocalDate.parse(rel.getDate().substring(0, 10)); // Sicurezza: prendiamo solo YYYY-MM-DD

            // se la release è uscita lo stesso giorno o dopo la data del ticket
            // le date sono gia' in ordine cronologico dalla piu' vecchia alla piu' recente
            if (!releaseDate.isBefore(ticketDate)) {
                return Integer.parseInt(rel.getReleaseIndex());
            }
        }

        // se il ticket e' stato chiuso dopo l'ultima release assegniamo l'indice dell'ultima
        String lastIndex = releases.get(releases.size() - 1).getReleaseIndex();
        return Integer.parseInt(lastIndex);
    }

    public static int getReleaseIndexByName(String versionName, List<ReleaseInfo> releases) {
        for (ReleaseInfo rel : releases) {
            if (rel.getReleaseID().equals(versionName)) {
                return Integer.parseInt(rel.getReleaseIndex());
            }
        }
        System.out.println(versionName);
        return -1; // Se la versione scritta su Jira non esiste nelle nostre release
    }

    // dato il commento di un commit torna la lista di ticketID presenti
    public static List<String> getTicketsFromComment(String comment, Set<String> bugTickets) {
        List<String> foundTickets = new ArrayList<>();
        // regex per trovare pattern come "OPENJPA-1234"
        Pattern pattern = Pattern.compile("OPENJPA-\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(comment);

        while (matcher.find()) {
            String ticketFound = matcher.group().toUpperCase();
            // aggiungiamo il ticket solo se fa parte della lista di ticket buggy
            if (bugTickets.contains(ticketFound)) {
                foundTickets.add(ticketFound);
            }
        }
        return foundTickets;
    }

    // ritona una mappa contenente: classPath -> lista di ticket bug che la riguardano
    public static Map<String, List<TicketBug>> mapBuggyFiles(Repository repository, List<TicketBug> validTickets) throws Exception {

        // mappa di lookup per trovare rapidamente l'oggetto TicketBug dall'ID stringa
        Map<String, TicketBug> ticketLookup = new HashMap<>();
        for (TicketBug t : validTickets) {
            ticketLookup.put(t.getKey(), t);
        }

        // risultato finale: (file path, lista dei ticket bug associati)
        Map<String, List<TicketBug>> fileToBugsMap = new HashMap<>();

        try (Git git = new Git(repository);
             DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            diffFormatter.setRepository(repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);

            // recupero tutti i commit di tutte le release
            Iterable<RevCommit> commits = git.log().all().call();

            for (RevCommit commit : commits) {
                String comment = commit.getFullMessage();

                // recupero gli ID dei ticket presenti nel commento del commit
                List<String> ticketIDs = getTicketsFromComment(comment, ticketLookup.keySet());

                // se non ci sono ticket bug, passo al prossimo commit
                // salto il primo commit (non ha padri per il confronto diff)
                if (ticketIDs.isEmpty() || commit.getParentCount() == 0) {
                    continue;
                }

                RevCommit parent = commit.getParent(0);

                // trovo i file modificati tra il commit corrente e il suo predecessore
                List<DiffEntry> diffs = diffFormatter.scan(parent.getTree(), commit.getTree());

                // Delego l'estrazione e il salvataggio dei file a un metodo privato
                processDiffsAndAssignTickets(diffs, ticketIDs, ticketLookup, fileToBugsMap);
            }
        }

        return fileToBugsMap;
    }

    //Analizza i diff e associa i file modificati (solo classi .java, ignorando i test) agli oggetti TicketBug pertinenti.
    private static void processDiffsAndAssignTickets(List<DiffEntry> diffs, List<String> ticketIDs, Map<String, TicketBug> ticketLookup, Map<String, List<TicketBug>> fileToBugsMap) {
        for (DiffEntry diff : diffs) {
            String filePath = diff.getNewPath();

            // prendo solo file .java e ignoro i test. Altrimenti passo al prossimo diff.
            if (filePath == null || !filePath.endsWith(".java") || filePath.toLowerCase().contains("test")) {
                continue;
            }

            for (String ticketId : ticketIDs) {
                TicketBug ticket = ticketLookup.get(ticketId);
                // aggiungo il ticket alla lista della classe
                fileToBugsMap.computeIfAbsent(filePath, k -> new ArrayList<>()).add(ticket);
            }
        }
    }

    public static void applySzzOracle(List<ClassMetrics> dataset, Map<String, List<TicketBug>> fileToBugsMap) {
        int classiBuggy = 0;

        for (ClassMetrics row : dataset) {
            String classPath = row.getFilePath(); // Corrisponde al filePath
            int releaseIndex = Integer.parseInt(row.getReleaseID()); // Indice della release per questa riga del dataset

            // recupero la lista di ticket buggy per questa classe
            List<TicketBug> bugsForThisClass = fileToBugsMap.get(classPath);

            if (bugsForThisClass != null) {
                for (TicketBug t : bugsForThisClass) {
                    // la classe é buggy se compresa tra la IV(compresa) e la FV(esclusa) di almeno un ticket bug
                    if (releaseIndex >= t.getIv() && releaseIndex < t.getFv()) {
                        row.setBuggy();
                        classiBuggy++;
                        break; // ne basta uno per etichettare la classe come Buggy
                    }
                }
            }
        }

        double percentuale = ((double) classiBuggy / dataset.size()) * 100;
        System.out.println("Labeling completato: trovate " + classiBuggy + " istanze Buggy nel dataset totale (" + dataset.size() + " righe, pari al " + String.format("%.2f", percentuale) + "%).");    }


}


