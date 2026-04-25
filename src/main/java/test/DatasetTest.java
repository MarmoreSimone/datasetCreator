package test;

import entity.ClassMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetTest {

    private DatasetTest() {}

    public static void testRows(List<ClassMetrics> dataset) {
        int errors = 0;

        for (int i = 0; i < dataset.size(); i++) {
            if (!isValidRow(dataset.get(i))) errors++;
        }

        System.out.println("righe con errori logici base: " + errors);
    }

    private static boolean isValidRow(ClassMetrics current) {

        // 1. Validazione LOC (Linee di codice)
        if (current.getLoc() <= 0 || current.getLocAddedPartial() < 0 || current.getLocAddedTotal() < 0) {
            return false;
        }

        // 2. Relazione Commit -> Autori
        if (current.getNrPartial() > 0 && current.getnAuthPartial() == 0) {
            return false;
        }

        // 3. Limite Fisico Autori/Commit
        if (current.getnAuthPartial() > current.getNrPartial()) {
            return false;
        }

        // 4. Relazione Commit -> LocAdded e Churn
        // Se non ci sono commit, non ci possono essere modifiche
        if (current.getNrPartial() == 0) {
            if (current.getLocAddedPartial() > 0 || current.getChurnPartial() > 0) {
                return false;
            }
        }

        // 5. Coerenza Parziale/Totale di Base
        if (current.getNrPartial() > current.getNrTotal() ||
                current.getnFixPartial() > current.getnFixTotal() ||
                current.getnAuthPartial() > current.getnAuthTotal() ||
                current.getLocAddedPartial() > current.getLocAddedTotal() ||
                current.getChurnPartial() > current.getChurnTotal()) { // NUOVO: Churn parziale non può superare il totale cumulativo
            return false;
        }

        // 6. Validazione Specifica CHURN
        // Il Churn è sempre >= LocAdded (perché Churn = Added + Modified + Deleted)
        if (current.getChurnPartial() < current.getLocAddedPartial() ||
                current.getChurnTotal() < current.getLocAddedTotal()) {
            return false;
        }

        // Il Max Churn non può superare il Churn Assoluto (nella stessa finestra temporale)
        if (current.getMaxChurnPartial() > current.getChurnPartial() ||
                current.getMaxChurnTotal() > current.getChurnTotal()) {
            return false;
        }

        // L'Avg Churn deve essere logicamente compreso tra il Max Churn e il minimo teorico (0)
        // Avg = Churn / NrCommit. Quindi se Churn > 0 e NrCommit > 0, Avg deve per forza essere <= MaxChurn.
        if (current.getAvgChurnPartial() > current.getMaxChurnPartial() ||
                current.getAvgChurnTotal() > current.getMaxChurnTotal()) {
            return false;
        }

        // Se passa tutti i controlli, la riga è internamente coerente
        return true;
    }

    public static boolean validateDatasetInMemory(List<ClassMetrics> dataset) {
        System.out.println("\n--- AVVIO VALIDAZIONE IN MEMORIA DEL DATASET ---");
        List<String> righeFallite = new ArrayList<>();

        // Mappa storica: NomeClasse -> (ReleaseID -> Metriche)
        Map<String, Map<String, ClassMetrics>> historyMap = new HashMap<>();

        for (int i = 0; i < dataset.size(); i++) {
            ClassMetrics current = dataset.get(i);
            int logicalRow = i + 1;

            // 1. TEST SULLA SINGOLA RIGA
            if (!isValidRow(current)) {
                righeFallite.add("[Elemento " + logicalRow + "] Errore Logico Singolo per: " + current.getFilePath() + " in release " + current.getReleaseID());
            }

            // 2. TEST STORICO GENEALOGICO E MATEMATICO
            String className = current.getFilePath();
            String currentRelease = current.getReleaseID();
            String predecessorRelease = current.getPredecessorID();

            if (predecessorRelease != null && !"NONE".equals(predecessorRelease)
                    && historyMap.containsKey(className)
                    && historyMap.get(className).containsKey(predecessorRelease)) {

                ClassMetrics past = historyMap.get(className).get(predecessorRelease);

                // A. EQUAZIONI ESATTE (Le somme devono combaciare perfettamente)
                if (current.getNrTotal() != past.getNrTotal() + current.getNrPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Matematica fallita (" + className + "): NrTotal non somma.");
                }

                if (current.getnFixTotal() != past.getnFixTotal() + current.getnFixPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Matematica fallita (" + className + "): nFixTotal non somma.");
                }

                if (current.getLocAddedTotal() != past.getLocAddedTotal() + current.getLocAddedPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Matematica fallita (" + className + "): LocAddedTotal non somma.");
                }

                // NUOVO: La somma del Churn Assoluto passato + parziale deve fare il totale
                if (current.getChurnTotal() != past.getChurnTotal() + current.getChurnPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Matematica fallita (" + className + "): ChurnTotal (" + current.getChurnTotal() + ") != past (" + past.getChurnTotal() + ") + partial (" + current.getChurnPartial() + ")");
                }

                // NUOVO: Il Max Churn Totale DEVE essere il maggiore tra il (Max Churn Passato) e il (Max Churn Parziale Corrente)
                int expectedMaxTotal = Math.max(past.getMaxChurnTotal(), current.getMaxChurnPartial());
                if (current.getMaxChurnTotal() != expectedMaxTotal) {
                    righeFallite.add("[Elemento " + logicalRow + "] Logica Max Churn fallita (" + className + "): MaxChurnTotal (" + current.getMaxChurnTotal() + ") non corrisponde al picco storico.");
                }

                // B. LOGICA DEGLI INSIEMI (Autori)
                if (current.getnAuthTotal() < past.getnAuthTotal() ||
                        current.getnAuthTotal() > past.getnAuthTotal() + current.getnAuthPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Logica Insiemi fallita (" + className + "): Incoerenza sugli autori unici rispetto al padre logico.");
                }
            }

            // Aggiorniamo la mappa storica
            historyMap.putIfAbsent(className, new HashMap<>());
            historyMap.get(className).put(currentRelease, current);
        }

        // --- STAMPA RISULTATI ---
        if (!righeFallite.isEmpty()) {
            System.err.println("❌ VALIDAZIONE FALLITA: Trovati " + righeFallite.size() + " errori prima dell'export.");
            for (int i = 0; i < Math.min(righeFallite.size(), 30); i++) {
                System.err.println("  - " + righeFallite.get(i));
            }
            if (righeFallite.size() > 30) System.err.println("  ... e altri " + (righeFallite.size() - 30) + " errori.");
            return false;
        } else {
            System.out.println("✅ VALIDAZIONE COMPLETATA CON SUCCESSO! I dati in memoria (incluso il Churn) sono matematicamente perfetti.");
            return true;
        }
    }
}
