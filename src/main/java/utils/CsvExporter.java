package utils;

import entity.ClassMetrics;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    private CsvExporter(){}

    private static final String HEADER = "ReleaseID,ClassName,LOC,NRtotal,NRpartial,NfixTotal,NfixPartial,NauthTotal,NauthPartial,locAddedTotal,locAddedPartial,Buggy";

    public static void exportToCsv(List<ClassMetrics> classMetrics, String outputDatasetPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDatasetPath))) {

            //header
            writer.write(HEADER);
            writer.newLine();

            //righe
            for (ClassMetrics metrics : classMetrics) {
                writer.write(metrics.toCsvRow());
                writer.newLine();
            }

            System.out.println("righe dataset: " + classMetrics.size());

        } catch (IOException e) {
            System.err.println("Errore durante la scrittura del CSV: " + e.getMessage());
        }
    }
}