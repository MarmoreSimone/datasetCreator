package acces_point;

import csv.CsvExporter;
import csv.CsvReader;
import entity.ClassMetrics;
import entity.ReleaseInfo;
import entity.TicketBug;
import labeling.Szz;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.*;
import java.util.*;
import java.util.stream.Collectors;
import metrics.ComputeMetrics;
import static labeling.Szz.applySzzOracle;
import static utils.MetricsUtils.countLocInClass;
import static utils.MetricsUtils.getJavaFilePaths;


public class Main {

    private static final String RELEASES_FILE_PATH = "src/main/java/outputs/OPENJPAVersionInfo.csv";//file generato dal codice di falessi
    private static final double RELEASES_PERCENTAGE = 0.34;//percentuale di release
    private static final double RELEASES_PERCENTAGE_FOR_SZZ = 1;//percentuale di release su cui calcolare proportion
    private static final String REPO_OPENJPA_PATH = "openjpa";
    private static final String OUTPUT_DATASET_PATH = "src/main/java/outputs/openjpa_dataset.csv";
    private static final String BUGGY_TICKET_PATH = "src/main/java/outputs/jiraTicketsEnriched.csv";

    public static void main(){
        try {
            List<ReleaseInfo> releases = CsvReader.getReleasesInfo(RELEASES_FILE_PATH, RELEASES_PERCENTAGE);
            List<ReleaseInfo> releasesForSzz = CsvReader.getReleasesInfo(RELEASES_FILE_PATH, RELEASES_PERCENTAGE_FOR_SZZ);
            List<TicketBug> buggyTicketsList = CsvReader.getTicketsFromCsv(BUGGY_TICKET_PATH,releasesForSzz);

            // FASE 1 SZZ: calcolo ov, fv, iv per i ticket che hanno le affected versions e uso proportion per gli altri, gli passo la lista di tutte le release del progetto
            Szz.completeSzz(buggyTicketsList,releasesForSzz);

            List<ClassMetrics> datasetFinale = Collections.synchronizedList(new ArrayList<>());

            // estraggo dalla List di buggyTickets un set che continene solo gli ID per questioni di ottimizzazione
            Set<String> buggyTicketsID = buggyTicketsList.stream().map(TicketBug::getKey).map(String::toUpperCase).collect(Collectors.toSet());

            try (Git git = GitUtils.openRepository(REPO_OPENJPA_PATH)) {

                // FASE 2 SZZ: creo una mappa dove per ogni classe .java ho tutti i ticket bug che la riguardano
                Map<String, List<TicketBug>> fileToBugsMap = Szz.mapBuggyFiles(git.getRepository(), buggyTicketsList);

                // recupero i tag delle release direttamente dal progetto
                List<String> gitTags = GitUtils.getAllGitTags(git);

                // cache per gli smell
                // mappa: tag della release -> mappa degli smell (classe -> numero di smell)
                Map<String, Map<String, Integer>> releaseSmellsCache = new HashMap<>();

                for (int i = 0; i < releases.size(); i++) {
                    ReleaseInfo rel = releases.get(i);
                    System.out.println("Analisi Release: " + rel.getReleaseID() + " (" + rel.getDate() + ")");

                    // trasformo, se serve, il tag nel formato interno al progetto
                    String currentTag = GitUtils.findMatchingTag(rel.getReleaseID(), gitTags);

                    // se non trova il tag salta la release
                    if (currentTag == null) {
                        System.out.println("Tag non trovato per la release " + rel.getReleaseID());
                        continue;
                    }

                    // uso checkoutToTag che mi porta sul commit esatto della release
                    RevCommit releaseCommit = GitUtils.checkoutToTag(git, currentTag);

                    // recupero tutti i classPath nella release i-esima
                    List<String> classPaths = getJavaFilePaths(REPO_OPENJPA_PATH);
                    System.out.println("Totale classi: " + classPaths.size());

                    final ObjectId currentReleaseId = releaseCommit.getId();

                    // trovo il predecessore logico
                    ReleaseInfo logicalPredecessor = GitUtils.findLogicalPredecessor(releases, i);
                    ObjectId tempPreviousReleaseHash = null;

                    String predTag = null;
                    // sonarCloud apprezza
                    if (logicalPredecessor != null && (predTag = GitUtils.findMatchingTag(logicalPredecessor.getReleaseID(), gitTags)) != null) {
                        System.out.println("Confronto: " + currentTag + " --> " + predTag);
                        tempPreviousReleaseHash = GitUtils.getObjectIdFromTag(git, predTag);
                    }

                    final ObjectId finalPreviousReleaseHash = tempPreviousReleaseHash;

                    Map<String, Integer> currentSmellsMapTemp = new HashMap<>();

                    // prendo gli smell del predecessore (se esiste)
                    if (predTag != null) {
                        // se non è in cache faccio il checkout calcolo e salvo gli smell nella cache
                        if (!releaseSmellsCache.containsKey(predTag)) {
                            System.out.println("Calcolo smell per il predecessore " + predTag + " (Cache miss)");
                            GitUtils.checkoutToTag(git, predTag);
                            releaseSmellsCache.put(predTag, MetricsUtils.getSmells(REPO_OPENJPA_PATH));
                            // torno alla release corrente
                            GitUtils.checkoutToTag(git, currentTag);
                        }
                        currentSmellsMapTemp = releaseSmellsCache.get(predTag);
                    }

                    final Map<String, Integer> currentSmellsMap = currentSmellsMapTemp;

                    // calcolo gli smell della release corrente
                    releaseSmellsCache.computeIfAbsent(currentTag, k -> MetricsUtils.getSmells(REPO_OPENJPA_PATH));

                    String predID = (logicalPredecessor != null) ? logicalPredecessor.getReleaseID() : "NONE";

                    // itero su tutte le classi
                    classPaths.parallelStream().forEach(filePath -> {
                        ClassMetrics metrics = new ClassMetrics(filePath, rel.getReleaseIndex(), rel.getReleaseID());
                        metrics.setLoc(countLocInClass(REPO_OPENJPA_PATH, filePath));
                        metrics.setPredecessorID(predID);
                        ComputeMetrics.computeMetrics(metrics, git, buggyTicketsID, currentReleaseId, finalPreviousReleaseHash, rel.getDate());

                        // imposto gli smell presi all'inizio (dal predecessore logico)
                        metrics.setSmells(currentSmellsMap.getOrDefault(filePath, 0));

                        datasetFinale.add(metrics);
                    });

                    System.out.println();
                }

                // FASE 3 SZZ: faccio il labeling finale usando iv e fv dei ticket buggy relativi ad una data classe
                applySzzOracle(datasetFinale, fileToBugsMap);

                // ripristino al master
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("master").call();
                System.out.println("Ripristino completato");

                CsvExporter.exportToCsv(datasetFinale, OUTPUT_DATASET_PATH);
            }
        } catch (Exception e) {
            System.err.println("Errore critico nell'esecuzione principale: " + e.getMessage());
        }
    }
}

