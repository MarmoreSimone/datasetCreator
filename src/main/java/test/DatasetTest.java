package test;

import entity.ClassMetrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetTest {

        private DatasetTest(){}

    public static void testDataset(List<ClassMetrics> dataset) {
        int errors = 0;
        int warnings = 0;
        // Mappa per confrontare la stessa classe tra release diverse (Monotonia)
        Map<String, ClassMetrics> lastSeenRelease = new HashMap<>();

        System.out.println("\n--- [AVVIO ANALISI INTEGRITÀ DATASET] ---");

        for (ClassMetrics current : dataset) {
            String name = current.getClassName();
            String relId = current.getReleaseID();

            // --- A. TEST COERENZA INTERNA (All'interno della stessa riga) ---

            // 1. Relazione Commit-Autori: Se c'è attività, deve esserci un responsabile
            if (current.getNrPartial() > 0 && current.getnAuthPartial() == 0) {
                System.err.println("❌ ERR [Auth]: " + name + " [Rel " + relId + "] ha " + current.getNrPartial() + " commit ma 0 autori parziali.");
                errors++;
            }

            // 2. Vincolo Fisico: Gli autori parziali non possono mai superare i commit parziali
            if (current.getnAuthPartial() > current.getNrPartial()) {
                System.err.println("❌ ERR [Limit]: " + name + " [Rel " + relId + "] ha più autori (" + current.getnAuthPartial() + ") che commit (" + current.getNrPartial() + ").");
                errors++;
            }

            // 3. Vincolo Storico (Generico): I valori parziali non devono superare i totali
            if (current.getNrPartial() > current.getNrTotal() ||
                    current.getnFixPartial() > current.getnFixTotal() ||
                    current.getnAuthPartial() > current.getnAuthTotal() ||
                    current.getLocAddedPartial() > current.getLocAddedTotal()) { // <-- AGGIUNTO
                System.err.println("❌ ERR [History]: Valori parziali superano i totali per " + name + " [Rel " + relId + "]");
                errors++;
            }

            // 4. Test Esistenza: Se il file è nel dataset, dovrebbe avere del codice
            if (current.getLoc() <= 0) {
                System.err.println("⚠️ WARN [LOC]: " + name + " ha LOC=0 nella Rel " + relId + ". (Controllare se git.clean() ha rimosso un file tracciato)");
                warnings++;
            }

            // 4.1 Valori Negativi Loc Added
            if (current.getLocAddedPartial() < 0 || current.getLocAddedTotal() < 0) {
                System.err.println("❌ ERR [Negative LOC]: Valori LocAdded negativi per " + name + " [Rel " + relId + "]");
                errors++;
            }

            // 4.2 Relazione Commit-LOC Added: Se 0 commit, allora 0 LOC aggiunte
            if (current.getNrPartial() == 0 && current.getLocAddedPartial() > 0) {
                System.err.println("❌ ERR [Commit-LOC]: " + name + " [Rel " + relId + "] ha 0 commit ma " + current.getLocAddedPartial() + " LOC aggiunte.");
                errors++;
            }

            // --- B. TEST DI EVOLUZIONE (Confronto con la release precedente dello stesso file) ---

            if (lastSeenRelease.containsKey(name)) {
                ClassMetrics prev = lastSeenRelease.get(name);

                // 5. Monotonia: I TOTALI non possono mai diminuire nel tempo
                if (current.getNrTotal() < prev.getNrTotal()) {
                    System.err.println("❌ ERR [Monotonia NR]: Storia diminuita per " + name + " (Rel " + prev.getReleaseID() + " -> " + relId + ")");
                    errors++;
                }
                if (current.getnAuthTotal() < prev.getnAuthTotal()) {
                    System.err.println("❌ ERR [Monotonia Nauth]: Persi autori storici per " + name);
                    errors++;
                }
                if (current.getLocAddedTotal() < prev.getLocAddedTotal()) {
                    System.err.println("❌ ERR [Monotonia LocAdded]: Il totale linee aggiunte è diminuito per " + name);
                    errors++;
                }

                // 6. Verifica Matematica Esatta (Somma rigida)

                // 6.1 NR: Totale DEVE essere la somma esatta
                if (current.getNrTotal() != (prev.getNrTotal() + current.getNrPartial())) {
                    System.err.println("❌ ERR [Math Exact NR]: Incoerenza per " + name + " (Tot: " + current.getNrTotal() + " != VecchioTot: " + prev.getNrTotal() + " + Parziale: " + current.getNrPartial() + ")");
                    errors++;
                }

                // 6.2 NFix: Totale DEVE essere la somma esatta
                if (current.getnFixTotal() != (prev.getnFixTotal() + current.getnFixPartial())) {
                    System.err.println("❌ ERR [Math Exact NFix]: Incoerenza per " + name);
                    errors++;
                }

                // 6.3 LocAdded: Totale DEVE essere la somma esatta
                if (current.getLocAddedTotal() != (prev.getLocAddedTotal() + current.getLocAddedPartial())) {
                    System.err.println("❌ ERR [Math Exact LocAdded]: Incoerenza per " + name + " (Tot: " + current.getLocAddedTotal() + " != VecchioTot: " + prev.getLocAddedTotal() + " + Parziale: " + current.getLocAddedPartial() + ")");
                    errors++;
                }

                // 6.4 NAuth: Essendo basato su Set (Insiemi), non si può usare l'uguaglianza stretta.
                // Il totale non può superare il vecchio totale + i nuovi parziali.
                if (current.getnAuthTotal() > (prev.getnAuthTotal() + current.getnAuthPartial())) {
                    System.err.println("❌ ERR [Math Set NAuth]: Autori in eccesso per " + name);
                    errors++;
                }
            }

            // Memorizzo questo stato per confrontarlo con la prossima release della stessa classe
            lastSeenRelease.put(name, current);
        }

        // --- REPORT FINALE ---
        System.out.println("------------------------------------------");
        if (errors == 0) {
            System.out.println("✅ ESITO: Il dataset è logicamente consistente.");
        } else {
            System.err.println("❌ ESITO: Trovati " + errors + " errori logici. Non usare questo dataset per ML.");
        }
        if (warnings > 0) {
            System.out.println("⚠️ NOTA: " + warnings + " avvisi rilevati.");
        }
        System.out.println("------------------------------------------\n");
    }

}
