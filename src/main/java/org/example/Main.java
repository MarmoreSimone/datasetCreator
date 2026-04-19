package org.example;

import entity.ClassMetrics;
import entity.ReleaseInfo;
import metrics.NR;
import metrics.Nfix;
import org.eclipse.jgit.api.Git;
import utils.CsvExporter;
import utils.GitUtils;
import utils.Miscellaneous;
import utils.csvReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import static metrics.ClassNames.getJavaClassesName;
import static metrics.CountLOC.countLoc;


public class Main {

    private static final String releasesFilePath = "OPENJPAVersionInfo.csv";//file generato dal codice di falessi
    private static final double releasesPercentage = 0.04;//percentuale di classi da prendere
    //private static final String repoOpenjpaPath = "C:/Users/simor/Desktop/openjpa";
    private static final String repoOpenjpaPath = "C:/Users/enrico/IdeaProjects/openjpa";
    private static final String outputDatasetPath = "openjpa_dataset.csv";

    private static String PREVIOUS_RELEASE_DATE = null;

    public static void main(String[] args) throws IOException{

        //recupero le release dal file generato dal codice del falessi
        List<ReleaseInfo> releases = csvReader.getReleasesInfo(releasesFilePath, releasesPercentage);

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

            //recupero il path di tutte le classi nella release i-esima che terminano con .java esclusi i test
            List<String> classPaths = getJavaClassesName(repoOpenjpaPath);
            System.out.println("   [INFO] Totale classi: " + classPaths.size());

            for (String percorsoClasse : classPaths) {

                ClassMetrics metrics = new ClassMetrics(percorsoClasse, rel.getReleaseID());//release ID + percorso file
                metrics.setLOC(countLoc(repoOpenjpaPath, percorsoClasse));//LOC
                metrics.setNRtotal(NR.TotalNR(git,percorsoClasse));//NRtotal
                metrics.setNRpartial(NR.PartialNR(git,percorsoClasse,PREVIOUS_RELEASE_DATE));//NRpartial
                metrics.setNfixTotal(Nfix.nFixTotal(percorsoClasse,buggyTicketsID,git));//NfixTotal
                metrics.setNfixPartial(Nfix.nFixPartial(percorsoClasse,buggyTicketsID,git,PREVIOUS_RELEASE_DATE));

                datasetFinale.add(metrics);
            }

            PREVIOUS_RELEASE_DATE = rel.getDate();
            System.out.println();
        }

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
