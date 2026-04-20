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

                // 3. Vincolo Storico: I valori parziali (della release) devono essere <= ai totali (storia intera)
                if (current.getNrPartial() > current.getNrTotal() ||
                        current.getnFixPartial() > current.getnFixTotal() ||
                        current.getnAuthPartial() > current.getnAuthTotal()) {
                    System.err.println("❌ ERR [History]: Valori parziali superano i totali per " + name);
                    errors++;
                }

                // 4. Test Esistenza: Se il file è nel dataset, dovrebbe avere del codice
                if (current.getLoc() <= 0) {
                    System.err.println("⚠️ WARN [LOC]: " + name + " ha LOC=0 nella Rel " + relId + ". (Controllare se git.clean() ha rimosso un file tracciato)");
                    warnings++;
                }

                // --- B. TEST DI EVOLUZIONE (Confronto con la release precedente dello stesso file) ---

                if (lastSeenRelease.containsKey(name)) {
                    ClassMetrics prev = lastSeenRelease.get(name);

                    // 5. Monotonia: NR, NFix e NAuth TOTALI non possono mai diminuire nel tempo
                    if (current.getNrTotal() < prev.getNrTotal()) {
                        System.err.println("❌ ERR [Monotonia NR]: Storia diminuita per " + name + " (Rel " + prev.getReleaseID() + " -> " + relId + ")");
                        errors++;
                    }
                    if (current.getnAuthTotal() < prev.getnAuthTotal()) {
                        System.err.println("❌ ERR [Monotonia Nauth]: Persi autori storici per " + name);
                        errors++;
                    }

                    // 6. Verifica Matematica: Il totale attuale deve essere ALMENO la somma del vecchio totale + parziale nuovo
                    if (current.getNrTotal() < (prev.getNrTotal() + current.getNrPartial())) {
                        System.err.println("❌ ERR [Math]: NRtotal incoerente per " + name + ". La somma dei parziali non corrisponde all'incremento del totale.");
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
                System.out.println("⚠️ NOTA: " + warnings + " avvisi LOC rilevati.");
            }
            System.out.println("------------------------------------------\n");
        }

}
