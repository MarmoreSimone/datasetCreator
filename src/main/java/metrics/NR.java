package metrics;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;

public class NR {

    //operazione che conta il numero di commit che hanno toccato il file dall'inizio del progetto fino alla revision attuale(compresa)
    public static int TotalNR(Git git, String FilePath) {
        int nr = 0;
        try {

            //head é l'identificativo del commit in cui mi trovo
            ObjectId head = git.getRepository().resolve("HEAD");

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

    //numero di commit relativi solo alla release corrente
    public static int PartialNR(Git git, String filePath, String previousReleaseDate) {
        int nr = 0;

        //caso prima release
        if (previousReleaseDate == null) {
            return TotalNR(git, filePath);
        }

        try {
            ObjectId head = git.getRepository().resolve("HEAD");

            Iterable<RevCommit> commits = git.log()
                    .add(head)
                    .addPath(filePath)
                    .setRevFilter(CommitTimeRevFilter.after(utils.Miscellaneous.toDate(previousReleaseDate)))//filtro solo per i commit di questa release
                    .call();

            for (RevCommit temp : commits) nr++;

        } catch (Exception e) {
            System.err.println("Errore durante il calcolo dell'NR per " + filePath + ": " + e.getMessage());
        }

        return nr;
    }

}