package test;

import entity.ClassMetrics;

import java.util.*;

public class DatasetTest {

        private DatasetTest(){}

    public static void testDataset(List<ClassMetrics> dataset) {
        int errors = 0;
        int warnings = 0;

        // Set per contare i percorsi file univoci che presentano ALMENO UN ERRORE
        Set<String> filesWithErrors = new HashSet<>();

        // Mappa per confrontare la stessa classe tra release diverse (Monotonia)
        Map<String, ClassMetrics> lastSeenRelease = new HashMap<>();

        System.out.println("\n--- [AVVIO ANALISI INTEGRITÀ DATASET] ---");

        for (ClassMetrics current : dataset) {
            String name = current.getFilePath();
            String relId = current.getReleaseID();

            // Flag per tracciare se questa specifica riga ha generato un errore
            boolean currentHasError = false;

            // --- A. TEST COERENZA INTERNA (All'interno della stessa riga) ---

            // 1. Relazione Commit-Autori
            if (current.getNrPartial() > 0 && current.getnAuthPartial() == 0) {
              //  System.err.println("❌ ERR [Auth]: " + name + " [Rel " + relId + "] ha " + current.getNrPartial() + " commit ma 0 autori parziali.");
                errors++;
                currentHasError = true;
            }

            // 2. Vincolo Fisico
            if (current.getnAuthPartial() > current.getNrPartial()) {
              //  System.err.println("❌ ERR [Limit]: " + name + " [Rel " + relId + "] ha più autori (" + current.getnAuthPartial() + ") che commit (" + current.getNrPartial() + ").");
                errors++;
                currentHasError = true;
            }

            // 3. Vincolo Storico (Generico)
            if (current.getNrPartial() > current.getNrTotal() ||
                    current.getnFixPartial() > current.getnFixTotal() ||
                    current.getnAuthPartial() > current.getnAuthTotal() ||
                    current.getLocAddedPartial() > current.getLocAddedTotal()) {
               // System.err.println("❌ ERR [History]: Valori parziali superano i totali per " + name + " [Rel " + relId + "]");
                errors++;
                currentHasError = true;
            }

            // 4. Test Esistenza (Questo è un WARN, quindi NON lo contiamo come errore per il Set)
            if (current.getLoc() <= 0) {
              //  System.err.println("⚠️ WARN [LOC]: " + name + " ha LOC=0 nella Rel " + relId + ". (Controllare se git.clean() ha rimosso un file tracciato)");
                warnings++;
            }

            // 4.1 Valori Negativi Loc Added
            if (current.getLocAddedPartial() < 0 || current.getLocAddedTotal() < 0) {
             //   System.err.println("❌ ERR [Negative LOC]: Valori LocAdded negativi per " + name + " [Rel " + relId + "]");
                errors++;
                currentHasError = true;
            }

            // 4.2 Relazione Commit-LOC Added
            if (current.getNrPartial() == 0 && current.getLocAddedPartial() > 0) {
              //  System.err.println("❌ ERR [Commit-LOC]: " + name + " [Rel " + relId + "] ha 0 commit ma " + current.getLocAddedPartial() + " LOC aggiunte.");
                errors++;
                currentHasError = true;
            }

            // --- B. TEST DI EVOLUZIONE (Confronto con la release precedente dello stesso file) ---

            if (lastSeenRelease.containsKey(name)) {
                ClassMetrics prev = lastSeenRelease.get(name);

                // 5. Monotonia
                if (current.getNrTotal() < prev.getNrTotal()) {
                  //  System.err.println("❌ ERR [Math Exact NR]: Incoerenza per " + name + " (Tot: " + current.getNrTotal() + " != VecchioTot: " + prev.getNrTotal() + " + Parziale: " + current.getNrPartial() + ")");
                    errors++;
                    currentHasError = true;
                }
                if (current.getnAuthTotal() < prev.getnAuthTotal()) {
                  //  System.err.println("❌ ERR [Monotonia Nauth]: Persi autori storici per " + name);
                    errors++;
                    currentHasError = true;
                }
                if (current.getLocAddedTotal() < prev.getLocAddedTotal()) {
                  //  System.err.println("❌ ERR [Monotonia LocAdded]: Il totale linee aggiunte è diminuito per " + name);
                    errors++;
                    currentHasError = true;
                }

                // 6. Verifica Matematica Esatta
                if (current.getNrTotal() != (prev.getNrTotal() + current.getNrPartial())) {
                   // System.err.println("❌ ERR [Math Exact NR]: Incoerenza per " + name + " (Tot: " + current.getNrTotal() + " != VecchioTot: " + prev.getNrTotal() + " + Parziale: " + current.getNrPartial() + ")");
                    errors++;
                    currentHasError = true;
                }

                if (current.getnFixTotal() != (prev.getnFixTotal() + current.getnFixPartial())) {
                   // System.err.println("❌ ERR [Math Exact NFix]: Incoerenza per " + name);
                    errors++;
                    currentHasError = true;
                }

                if (current.getLocAddedTotal() != (prev.getLocAddedTotal() + current.getLocAddedPartial())) {
                   // System.err.println("❌ ERR [Math Exact LocAdded]: Incoerenza per " + name + " (Tot: " + current.getLocAddedTotal() + " != VecchioTot: " + prev.getLocAddedTotal() + " + Parziale: " + current.getLocAddedPartial() + ")");
                    errors++;
                    currentHasError = true;
                }

                if (current.getnAuthTotal() > (prev.getnAuthTotal() + current.getnAuthPartial())) {
                   // System.err.println("❌ ERR [Math Set NAuth]: Autori in eccesso per " + name);
                    errors++;
                    currentHasError = true;
                }
            }

            // Se è stato trovato almeno un errore in questo file per questa release, lo aggiungiamo al Set
            if (currentHasError) {
                filesWithErrors.add(name);
            }

            // Memorizzo questo stato per confrontarlo con la prossima release della stessa classe
            lastSeenRelease.put(name, current);
        }

        // --- REPORT FINALE AGGIORNATO ---
        System.out.println("------------------------------------------");

        if (errors == 0) {
            System.out.println("✅ ESITO: Il dataset è logicamente consistente.");
        } else {
            System.err.println("❌ ESITO: Trovati " + errors + " errori logici in " + filesWithErrors.size() + " file univoci. Non usare questo dataset per ML.");
        }

        if (warnings > 0) {
            System.out.println("⚠️ NOTA: " + warnings + " avvisi rilevati.");
        }
        System.out.println("------------------------------------------\n");
    }

    public static void testRows(List<ClassMetrics> dataset){
        int errors = 0;

        for(int i=0;i<dataset.size();i++){
            if(isValidRow(dataset.get(i)) == false) errors++;
        }

        System.out.println("righe con errori: " + errors);

    }

    private static boolean isValidRow(ClassMetrics current) {

        // 1. Validazione LOC (Linee di codice)
        // Se la classe esiste nel dataset, deve avere almeno 1 riga di codice.
        // E i valori aggiunti non possono mai essere negativi.
        if (current.getLoc() <= 0 || current.getLocAddedPartial() < 0 || current.getLocAddedTotal() < 0) {
            return false;
        }

        // 2. Relazione Commit -> Autori
        // Se c'è almeno 1 commit parziale, deve esserci almeno 1 autore parziale.
        if (current.getNrPartial() > 0 && current.getnAuthPartial() == 0) {
            return false;
        }

        // 3. Limite Fisico Autori/Commit
        // Gli autori parziali (persone diverse) non possono essere numericamente
        // superiori al totale dei commit parziali effettuati.
        if (current.getnAuthPartial() > current.getNrPartial()) {
            return false;
        }

        // 4. Relazione Commit -> LocAdded
        // Se non ci sono stati commit (NrPartial = 0), non possono essere state
        // aggiunte nuove linee di codice in quella specifica release.
        if (current.getNrPartial() == 0 && current.getLocAddedPartial() > 0) {
            return false;
        }

        // 5. Coerenza Parziale/Totale
        // I valori parziali (fatti in questa release) non possono logicamente
        // superare i totali cumulativi accumulati fino a questa release.
        if (current.getNrPartial() > current.getNrTotal() ||
                current.getnFixPartial() > current.getnFixTotal() ||
                current.getnAuthPartial() > current.getnAuthTotal() ||
                current.getLocAddedPartial() > current.getLocAddedTotal()) {
            return false;
        }

        // Se passa tutti i controlli, la riga è internamente coerente
        return true;
    }

}
