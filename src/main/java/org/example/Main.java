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
import java.util.Collections;
import java.util.List;
import java.util.Set;


import metrics.ComputeMetrics;

import static metrics.ClassNames.getJavaClassesName;
import static metrics.CountLOC.countLoc;
import static test.DatasetTest.testDataset;


public class Main {

    private static final String RELEASES_FILE_PATH = "OPENJPAVersionInfo.csv";//file generato dal codice di falessi
    private static final double RELEASES_PERCENTAGE = 0.34;//percentuale di classi da prendere
    private static final String REPO_OPENJPA_PATH = "openjpa";
    private static final String OUTPUT_DATASET_PATH = "openjpa_dataset.csv";

    private static String previousReleaseDate = null;

    public static void main2() throws IOException, GitAPIException {

        //recupero le release dal file generato dal codice del falessi
        List<ReleaseInfo> releases = CsvReader.getReleasesInfo(RELEASES_FILE_PATH, RELEASES_PERCENTAGE);

        //recupero gli id dei ticket buggy
        Set<String> buggyTicketsID = Miscellaneous.retrieveTicketsID();

        //apro la repo di OpenJPA
        Git git = GitUtils.openRepository(REPO_OPENJPA_PATH);

        //lista che contiene tutte le classi con relative metriche
        List<ClassMetrics> datasetFinale = new ArrayList<>();

        for (ReleaseInfo rel : releases) {
            System.out.println("--- Analisi Release: " + rel.getReleaseName() + " (" + rel.getDate() + ") ---");

            //faccio il checkout della release i-esima e prendo il primo commit antecedente alla data della release
            GitUtils.checkoutToDate(git, rel.getDate());

            git.clean().setCleanDirectories(true).setForce(true).call();

            //recupero il path di tutte le classi nella release i-esima che terminano con .java esclusi i test
            List<String> classPaths = getJavaClassesName(REPO_OPENJPA_PATH);
            System.out.println("   [INFO] Totale classi: " + classPaths.size());

            for (String percorsoClasse : classPaths) {

                ClassMetrics metrics = new ClassMetrics(percorsoClasse, rel.getReleaseID());//release ID + percorso file
                metrics.setLoc(countLoc(REPO_OPENJPA_PATH, percorsoClasse));//LOC
                ComputeMetrics.setMetrics(metrics,git,buggyTicketsID, previousReleaseDate);//tutte le altre metriche

                datasetFinale.add(metrics);
            }

            previousReleaseDate = rel.getDate();
            System.out.println();
        }

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
        CsvExporter.exportToCsv(datasetFinale, OUTPUT_DATASET_PATH);

    }

        public static void main() { // Rimosso throws generico per gestirlo meglio

            try {

                //recupero le release dal file generato dal codice del falessi
                List<ReleaseInfo> releases = CsvReader.getReleasesInfo(RELEASES_FILE_PATH, RELEASES_PERCENTAGE);

                //recupero gli id dei ticket buggy
                Set<String> buggyTicketsID = Miscellaneous.retrieveTicketsID();

                //lista che contiene tutte le classi con relative metriche, acchittata per il parallelismo
                List<ClassMetrics> datasetFinale = Collections.synchronizedList(new ArrayList<>());

                //apro la repo di OpenJPA
                try (Git git = GitUtils.openRepository(REPO_OPENJPA_PATH)) {

                    for (ReleaseInfo rel : releases) {
                        System.out.println("--- Analisi Release: " + rel.getReleaseName() + " (" + rel.getDate() + ") ---");

                        //faccio il checkout della release i-esima e prendo il primo commit antecedente alla data della release
                        GitUtils.checkoutToDate(git, rel.getDate());

                        //pulisco eventuali file 'fantasma'
                        git.clean().setCleanDirectories(true).setForce(true).call();

                        //recupero il path di tutte le classi nella release i-esima che terminano con .java esclusi i test
                        List<String> classPaths = getJavaClassesName(REPO_OPENJPA_PATH);
                        System.out.println("   [INFO] Totale classi: " + classPaths.size());

                        // 3. Elaborazione in parallelo delle classi
                        // NOTA: previousReleaseDate deve essere passato come parametro "final" o copiato in una variabile locale per le lambda
                        final String currentPreviousDate = previousReleaseDate;

                        classPaths.parallelStream().forEach(percorsoClasse -> {

                            ClassMetrics metrics = new ClassMetrics(percorsoClasse, rel.getReleaseID());//release ID + percorso file
                            // Assumo che countLoc legga il file dal disco (ora che abbiamo fatto il checkout)
                            metrics.setLoc(countLoc(REPO_OPENJPA_PATH, percorsoClasse));//LOC
                            ComputeMetrics.setMetrics(metrics, git, buggyTicketsID, currentPreviousDate);//tutte le altre metriche

                            datasetFinale.add(metrics);
                        });

                        previousReleaseDate = rel.getDate();
                        System.out.println();
                    }

                    // RIPRISTINO AL MASTER (Fatto dentro il blocco try, prima che Git si chiuda)
                    System.out.println("Ripristino repository al branch master...");
                    git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("master").call();
                    System.out.println("Ripristino completato.");

                } // git.close() viene chiamato automaticamente qui

                testDataset(datasetFinale);

                //scrivo i dati sul csv finale
                CsvExporter.exportToCsv(datasetFinale, OUTPUT_DATASET_PATH);

            } catch (Exception e) {
                System.err.println("Errore critico nell'esecuzione principale: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

