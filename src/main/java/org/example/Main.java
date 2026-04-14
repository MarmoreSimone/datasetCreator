package org.example;

import entity.ClassMetrics;
import entity.ReleaseInfo;
import org.eclipse.jgit.api.Git;
import temporary.ConsoleCsvPrinter;
import utils.CsvExporter;
import utils.GitUtils;
import utils.csvReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import static metrics.ClassNames.getJavaClassesName;
import static metrics.CountLOC.countLoc;
import static metrics.NRtotal.NRtotal;

public class Main {

    /*
    private static final String releasesFilePath = "C:/Users/simor/Desktop/progetto falessi/OPENJPAVersionInfo.csv";//file generato dal codice di falessi
    private static final double releasesPercentage = 0.02;//percentuale di classi da prendere
    private static final String repoOpenjpaPath = "C:/Users/simor/Desktop/openjpa";
    private static final String outputDatasetPath = "C:/Users/simor/Desktop/datasetCreator/openjpa_dataset.csv";
    */

    private static final String releasesFilePath = "C:/Users/enrico/IdeaProjects/datasetCreator/OPENJPAVersionInfo.csv";//file generato dal codice di falessi
    private static final double releasesPercentage = 0.02;//percentuale di classi da prendere
    private static final String repoOpenjpaPath = "C:/Users/enrico/IdeaProjects/openjpa";
    private static final String outputDatasetPath = "C:/Users/enrico/IdeaProjects/datasetCreator/openjpa_dataset.csv";
    public static void main(String[] args) throws IOException{

        List<ReleaseInfo> releases = csvReader.getReleasesInfo(releasesFilePath, releasesPercentage);
        Git git = GitUtils.openRepository(repoOpenjpaPath);
        List<ClassMetrics> datasetFinale = new ArrayList<>();

        for (ReleaseInfo rel : releases) {
            System.out.println("--- Analisi Release: " + rel.getReleaseName() + " (" + rel.getDate() + ") ---");

            //faccio il checkout della release i-esima
            GitUtils.checkoutToDate(git, rel.getDate());

            //stampiamo i risultati per questa release
            List<String> nomiClassi = getJavaClassesName(repoOpenjpaPath);
            System.out.println("   [INFO] Totale classi: " + nomiClassi.size());

            for (String nomeClasse : nomiClassi) {

                // 1. Crea l'istanza usando il costruttore (Nome Classe, ID Release)
                ClassMetrics metrics = new ClassMetrics(nomeClasse, rel.getReleaseID());//release ID + nome
                metrics.setLOC(countLoc(repoOpenjpaPath, nomeClasse));//LOC
                metrics.setNRtotal(NRtotal(git, repoOpenjpaPath, rel.getDate()));//NRtotal

                // 2. Salva l'istanza nella lista globale
                datasetFinale.add(metrics);
            }

            System.out.println();

        }

        //ConsoleCsvPrinter.printDataset(datasetFinale);

        // 4. RITORNO AL MASTER
        System.out.println("Ripristino repository al branch master...");
        try {
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("master").call();
            System.out.println("Ripristino completato.");
        } catch (Exception e) {
            System.err.println("Errore nel tornare al master: " + e.getMessage());
        }

        //scrivo dati sul csv
        CsvExporter.exportToCsv(datasetFinale,outputDatasetPath);

    }
}
