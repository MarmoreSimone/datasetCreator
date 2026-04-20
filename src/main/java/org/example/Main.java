package org.example;

import entity.ClassMetrics;
import entity.ReleaseInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import utils.CsvExporter;
import utils.GitUtils;
import utils.Miscellaneous;
import utils.CsvReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import metrics.ComputeMetrics;

import static metrics.ClassNames.getJavaClassesName;
import static metrics.CountLOC.countLoc;
import static test.DatasetTest.testDataset;


public class Main {

    private static final String releasesFilePath = "OPENJPAVersionInfo.csv";//file generato dal codice di falessi
    private static final double releasesPercentage = 0.03;//percentuale di classi da prendere
    //private static final String repoOpenjpaPath = "C:/Users/simor/Desktop/openjpa";
    private static final String repoOpenjpaPath = "openjpa";
    //private static final String repoOpenjpaPath = "C:/Users/enrico/IdeaProjects/openjpa";
    private static final String outputDatasetPath = "openjpa_dataset.csv";

    private static String previousReleaseDate = null;

    public static void main(String[] args) throws IOException, GitAPIException {

        //recupero le release dal file generato dal codice del falessi
        List<ReleaseInfo> releases = CsvReader.getReleasesInfo(releasesFilePath, releasesPercentage);

        //recupero gli id dei ticket buggy
        HashSet<String> buggyTicketsID = Miscellaneous.retrieveTicketsID();

        //apro la repo di OpenJPA
        Git git = GitUtils.openRepository(repoOpenjpaPath);

        //lista che contiene tutte le classi con relative metriche
        List<ClassMetrics> datasetFinale = new ArrayList<>();


        for (ReleaseInfo rel : releases) {
            System.out.println("--- Analisi Release: " + rel.getReleaseName() + " (" + rel.getDate() + ") ---");

            //faccio il checkout della release i-esima e prendo il primo commit antecedente alla data della release
            GitUtils.checkoutToDate(git, rel.getDate());

            git.clean().setCleanDirectories(true).setForce(true).call();

            //recupero il path di tutte le classi nella release i-esima che terminano con .java esclusi i test
            List<String> classPaths = getJavaClassesName(repoOpenjpaPath);
            System.out.println("   [INFO] Totale classi: " + classPaths.size());

            for (String percorsoClasse : classPaths) {

                ClassMetrics metrics = new ClassMetrics(percorsoClasse, rel.getReleaseID());//release ID + percorso file
                metrics.setLoc(countLoc(repoOpenjpaPath, percorsoClasse));//LOC
                ComputeMetrics.setMetrics(metrics,git,buggyTicketsID, previousReleaseDate);//tutte le altre metriche

                datasetFinale.add(metrics);
            }

            previousReleaseDate = rel.getDate();
            System.out.println();
        }

        /*
        for (ReleaseInfo rel : releases) {
            System.out.println("--- Analisi Release: " + rel.getReleaseName() + " (" + rel.getDate() + ") ---");

            GitUtils.checkoutToDate(git, rel.getDate());

            try {
                git.clean().setCleanDirectories(true).setForce(true).call();
            } catch (Exception e) {
                System.err.println("   [WARNING] Impossibile pulire: " + e.getMessage());
            }

            List<String> classPaths = getJavaClassesName(repoOpenjpaPath);
            System.out.println("   [INFO] Totale classi: " + classPaths.size());

            // 1. "Congelo" la data per poterla passare sicura ai thread
            final String threadSafePreviousDate = PREVIOUS_RELEASE_DATE;

            // 2. AVVIO IL PARALLELISMO
            classPaths.parallelStream().forEach(percorsoClasse -> {

                ClassMetrics metrics = new ClassMetrics(percorsoClasse, rel.getReleaseID());
                metrics.setLOC(countLoc(repoOpenjpaPath, percorsoClasse));

                // 3. Ogni thread apre il SUO "clone" di Git per non pestarsi i piedi
                try (Git threadGit = Git.open(new File(repoOpenjpaPath))) {

                    ComputeMetrics.setMetrics(metrics, threadGit, buggyTicketsID, threadSafePreviousDate);

                } catch (Exception e) {
                    System.err.println("Errore nell'apertura di Git per il thread: " + e.getMessage());
                }

                // 4. Sincronizzo l'aggiunta alla lista: entra un thread alla volta
                synchronized (datasetFinale) {
                    datasetFinale.add(metrics);
                }
            });

            PREVIOUS_RELEASE_DATE = rel.getDate();
            System.out.println();
        }
         */

        //RITORNO AL MASTER
        System.out.println("Ripristino repository al branch master...");
        try {
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("master").call();
            System.out.println("Ripristino completato.");
        } catch (Exception e) {
            System.err.println("Errore nel tornare al master: " + e.getMessage());
        }

        testDataset(datasetFinale);

        //scrivo dati sul csv
        CsvExporter.exportToCsv(datasetFinale,outputDatasetPath);

    }
}
