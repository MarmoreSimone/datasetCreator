package utils;

import entity.ClassMetrics;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    private static final String HEADER = "ReleaseID,ClassName,LOC,NRtotal,Buggy";

    /**
     * Esporta una lista di ClassMetrics in un file CSV.
     * @param metricsList La lista dei dati da scrivere.
     * @param outputDatasetPath Il percorso completo del file (es. "C:/.../dataset.csv").
     */
    public static void exportToCsv(List<ClassMetrics> metricsList, String outputDatasetPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDatasetPath))) {

            // 1. Scrittura Header
            writer.write(HEADER);
            writer.newLine();

            // 2. Scrittura Righe
            for (ClassMetrics metrics : metricsList) {
                writer.write(metrics.toCsvRow());
                writer.newLine();
            }

            System.out.println("Dataset creato con successo: " + outputDatasetPath);
            System.out.println("Totale righe: " + metricsList.size());

        } catch (IOException e) {
            System.err.println("Errore critico durante la scrittura del CSV: " + e.getMessage());
        }
    }
}