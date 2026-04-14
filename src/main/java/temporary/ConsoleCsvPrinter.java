package temporary;

import entity.ClassMetrics;

import java.util.List;

public class ConsoleCsvPrinter {

    /**
     * Stampa l'intestazione del CSV per far capire quali sono le colonne.
     */
    public static void printHeader() {
        System.out.println("ReleaseID, ClassName, LOC, NRtotal");
        System.out.println("-------------------------------------------------------");
    }

    /**
     * Stampa una singola riga. Usa i getter per prendere solo Nome e Release.
     */
    public static void printRow(ClassMetrics metrics) {
        // %s indica una stringa, %n va a capo in modo compatibile con ogni sistema operativo
        System.out.printf("%s -- %s -- %d -- %d%n", metrics.getReleaseID(), metrics.getClassName(), metrics.getLOC(), metrics.getNRtotal());
    }

    /**
     * Metodo "tutto in uno": gli passi la lista e lui stampa l'intero dataset.
     */
    public static void printDataset(List<ClassMetrics> dataset) {
        System.out.println("\n=== INIZIO ANTEPRIMA DATASET CSV ===");
        printHeader();

        for (ClassMetrics riga : dataset) {
            printRow(riga);
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("=== FINE ANTEPRIMA (Totale righe: " + dataset.size() + ") ===\n");
    }
}