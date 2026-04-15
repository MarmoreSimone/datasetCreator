package metrics;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

public class NR {

    //operazione che conta il numero di commit che hanno toccato il file dall'inizio del progetto fino alla revision attuale
    public static int TotalNR(Git git, String FilePath) {
        int nr = 0;
        try {

            //head é l'identificativo del commit in cui mi trovo
            ObjectId head = git.getRepository().resolve("HEAD");
            if (head == null) {
                System.err.println("Attenzione: HEAD non trovato. Sei sicuro di aver fatto il checkout?");
                return 0;
            }

            //prendo tutti i commit relativi a quel file
            Iterable<RevCommit> commits = git.log()
                    .add(head)//parte da qui e va indietro
                    .addPath(FilePath)//prendo solo i commit che hanno toccato il file di interesse
                    .call();

            //conto i commit ritornati in commits
            for (RevCommit temp : commits) nr++;

        } catch (Exception e) {
            System.err.println("Errore durante il calcolo dell'NR per " + FilePath + ": " + e.getMessage());
        }

        return nr;
    }

}