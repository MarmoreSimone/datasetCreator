package test;

import entity.ClassMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetTest {

    private DatasetTest() {}

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

        // 4. Relazione Commit -> LocAdded, Churn e ChangeSet
        if (current.getNrPartial() == 0) {
            if (current.getLocAddedPartial() > 0 || current.getChurnPartial() > 0 || current.getChgSetPartial() > 0) {
                return false;
            }
        }

        // 5. Coerenza Parziale/Totale di Base
        if (current.getNrPartial() > current.getNrTotal() ||
                current.getnFixPartial() > current.getnFixTotal() ||
                current.getnAuthPartial() > current.getnAuthTotal() ||
                current.getLocAddedPartial() > current.getLocAddedTotal() ||
                current.getChurnPartial() > current.getChurnTotal() ||
                current.getChgSetPartial() > current.getChgSetTotal()) { // NUOVO: ChgSet
            return false;
        }

        // 6. Validazione Specifica CHURN
        if (current.getChurnPartial() < current.getLocAddedPartial() ||
                current.getChurnTotal() < current.getLocAddedTotal()) {
            return false;
        }

        if (current.getMaxChurnPartial() > current.getChurnPartial() ||
                current.getMaxChurnTotal() > current.getChurnTotal()) {
            return false;
        }

        if (current.getAvgChurnPartial() > current.getMaxChurnPartial() ||
                current.getAvgChurnTotal() > current.getMaxChurnTotal()) {
            return false;
        }

        // 7. NUOVO: Validazione AGE (L'età non può mai essere negativa)
        if (current.getAge() < 0) {
            return false;
        }

        // 8. NUOVO: Validazione Specifica CHANGE SET
        // Poiché ogni commit tocca almeno la classe che stiamo analizzando,
        // il totale dei file toccati (ChgSet) deve essere >= al numero di commit.
        if (current.getNrPartial() > 0 && current.getChgSetPartial() < current.getNrPartial()) {
            return false;
        }
        if (current.getNrTotal() > 0 && current.getChgSetTotal() < current.getNrTotal()) {
            return false;
        }

        // 9. Validazione Fisica delle LOC
        if (current.getLoc() > current.getLocAddedTotal()) {
            return false;
        }

        // Il Max ChgSet non può superare la somma assoluta
        if (current.getMaxChgSetPartial() > current.getChgSetPartial() ||
                current.getMaxChgSetTotal() > current.getChgSetTotal()) {
            return false;
        }

        // L'Avg ChgSet deve essere <= al Max ChgSet
        if (current.getAvgChangeSetPartial() > current.getMaxChgSetPartial() ||
                current.getAvgChangeSetTotal() > current.getMaxChgSetTotal()) {
            return false;
        }

        // Se passa tutti i controlli, la riga è internamente coerente
        return true;
    }

    public static boolean validateDatasetInMemory(List<ClassMetrics> dataset) {
        System.out.println("\n--- AVVIO VALIDAZIONE IN MEMORIA DEL DATASET ---");
        List<String> righeFallite = new ArrayList<>();

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

                if (current.getChurnTotal() != past.getChurnTotal() + current.getChurnPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Matematica fallita (" + className + "): ChurnTotal non somma.");
                }

                // NUOVO: La somma del Change Set passato + parziale deve fare il totale
                if (current.getChgSetTotal() != past.getChgSetTotal() + current.getChgSetPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Matematica fallita (" + className + "): ChgSetTotal non somma.");
                }

                // B. LOGICA DEI MASSIMI STORICI
                int expectedMaxChurnTotal = Math.max(past.getMaxChurnTotal(), current.getMaxChurnPartial());
                if (current.getMaxChurnTotal() != expectedMaxChurnTotal) {
                    righeFallite.add("[Elemento " + logicalRow + "] Logica Max Churn fallita (" + className + "): MaxChurnTotal non corrisponde al picco storico.");
                }

                // NUOVO: Il Max Change Set Totale deve derivare storicamente dal passato o dal parziale corrente
                int expectedMaxChgTotal = Math.max(past.getMaxChgSetTotal(), current.getMaxChgSetPartial());
                if (current.getMaxChgSetTotal() != expectedMaxChgTotal) {
                    righeFallite.add("[Elemento " + logicalRow + "] Logica Max ChgSet fallita (" + className + "): MaxChgSetTotal non corrisponde al picco storico.");
                }

                // C. LOGICA DEGLI INSIEMI E TEMPO
                if (current.getnAuthTotal() < past.getnAuthTotal() ||
                        current.getnAuthTotal() > past.getnAuthTotal() + current.getnAuthPartial()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Logica Insiemi fallita (" + className + "): Incoerenza sugli autori.");
                }

                // NUOVO: Il codice non può viaggiare indietro nel tempo.
                // L'età della classe nella release corrente DEVE essere >= all'età nella release precedente.
                if (current.getAge() < past.getAge()) {
                    righeFallite.add("[Elemento " + logicalRow + "] Logica Tempo fallita (" + className + "): L'Age della classe è diminuita rispetto alla release precedente.");
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
            System.out.println("✅ VALIDAZIONE COMPLETATA CON SUCCESSO! I dati in memoria (inclusi Age e ChgSet) sono matematicamente perfetti.");
            return true;
        }
    }
}
