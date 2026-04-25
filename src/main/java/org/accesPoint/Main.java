package org.accesPoint;

import csv.CsvExporter;
import csv.CsvReader;
import entity.ClassMetrics;
import entity.ReleaseInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.*;

import java.util.*;
import metrics.ComputeMetrics;

import static test.DatasetTest.testRows;
import static test.DatasetTest.validateDatasetInMemory;
import static utils.MetricsUtils.countLocInClass;
import static utils.MetricsUtils.getJavaFilePaths;


public class Main {

    private static final String RELEASES_FILE_PATH = "OPENJPAVersionInfo.csv";//file generato dal codice di falessi
    private static final double RELEASES_PERCENTAGE = 0.34;//percentuale di classi da prendere
    private static final String REPO_OPENJPA_PATH = "openjpa";
    private static final String OUTPUT_DATASET_PATH = "openjpa_dataset.csv";

    public static void main(){
        try {
            List<ReleaseInfo> releases = CsvReader.getReleasesInfo(RELEASES_FILE_PATH, RELEASES_PERCENTAGE);
            Set<String> buggyTicketsID = CsvReader.retrieveTicketsID();
            List<ClassMetrics> datasetFinale = Collections.synchronizedList(new ArrayList<>());

            try (Git git = GitUtils.openRepository(REPO_OPENJPA_PATH)) {

                // recupero i tag delle release direttamente dal progetto
                List<String> gitTags = GitUtils.getAllGitTags(git);

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
                    final ObjectId currentReleaseId = releaseCommit.getId();

                    // trovo il predecessore logico
                    ReleaseInfo logicalPredecessor = GitUtils.findLogicalPredecessor(releases, i);
                    ObjectId tempPreviousReleaseHash = null;

                    if (logicalPredecessor != null) {
                        String predTag = GitUtils.findMatchingTag(logicalPredecessor.getReleaseID(), gitTags);
                        if (predTag != null) {
                            System.out.println("Confronto: " + currentTag + " --> " + predTag);
                            tempPreviousReleaseHash = GitUtils.getObjectIdFromTag(git, predTag);
                        }
                    } else {
                        System.out.println("Nessun predecessore logico (Questa è la primissima release!).");
                    }

                    final ObjectId finalPreviousReleaseHash = tempPreviousReleaseHash;

                    List<String> classPaths = getJavaFilePaths(REPO_OPENJPA_PATH);
                    System.out.println("Totale classi: " + classPaths.size());
                    //todo
                    //togli
                    String predID = (logicalPredecessor != null) ? logicalPredecessor.getReleaseID() : "NONE";

                    classPaths.parallelStream().forEach(filePath -> {
                        ClassMetrics metrics = new ClassMetrics(filePath, rel.getReleaseIndex(), rel.getReleaseID());
                        metrics.setLoc(countLocInClass(REPO_OPENJPA_PATH, filePath));
                        //todo
                        //togli serve per il test, il predecessor
                        metrics.setPredecessorID(predID);
                        ComputeMetrics.computeMetrics(metrics, git, buggyTicketsID, currentReleaseId, finalPreviousReleaseHash);
                        datasetFinale.add(metrics);
                    });

                    System.out.println();
                }

                // ripristino al master
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("master").call();
                System.out.println("Ripristino completato");

                validateDatasetInMemory(datasetFinale);
                CsvExporter.exportToCsv(datasetFinale, OUTPUT_DATASET_PATH);
            }

        } catch (Exception e) {
            System.err.println("Errore critico nell'esecuzione principale: " + e.getMessage());
        }
    }
}

