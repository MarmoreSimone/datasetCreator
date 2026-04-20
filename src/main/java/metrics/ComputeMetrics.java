package metrics;

import entity.ClassMetrics;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.MetricsUtils;

import java.util.HashSet;

public class ComputeMetrics {

    public static void setMetrics(ClassMetrics metrics, Git git, HashSet<String> buggyTicketList, String previousReleaseDate){

        long previousDateTime = 0;
        if (previousReleaseDate != null) {
            previousDateTime = utils.Miscellaneous.toDate(previousReleaseDate).getTime();//praticamente faccio String -> Date -> Long
        }

        boolean isPartial;

        int nr = 0;
        int nrPartial = 0;
        int nFix = 0;
        int nFixPartial = 0;

        //uso un HashSet cosi da non dover gestire i duplicati
        HashSet<String> nAuthTotal = new HashSet<>();
        HashSet<String> nAuthPartial = new HashSet<>();

        try {

            //head é l'identificativo del commit in cui mi trovo
            ObjectId head = git.getRepository().resolve("HEAD");

            Iterable<RevCommit> commits = git.log()//prendo tutti i commit relativi a quel file
                    .add(head)//parte da qui e va indietro
                    .addPath(metrics.getClassName())//prendo solo i commit che hanno toccato il file di interesse
                    .call();

            for (RevCommit commit : commits){

                long commitDate = commit.getCommitTime() * 1000L;
                if(commitDate > previousDateTime) isPartial = true;
                else isPartial = false;

                //1) parte relativa a NR
                //conta il numero di volte che una classe é stata toccata da un commit relativo a un ticket di tipo BUG, total é relativo a tutta la vita della classe
                nr++;
                if(isPartial) nrPartial++;

                //2) parte relativa a Nfix
                //conta il numero di volte che una classe é stata toccata da un commit relativo a un ticket di tipo BUG, total é relativo a tutta la vita della classe
                String message = commit.getFullMessage();//recupero il commento del commit
                if (MetricsUtils.isCommitAFix(message, buggyTicketList)) {//controllo se nel messaggio c'é il riferimento a un ticket buggy
                    //System.out.println(message+filePath);
                    nFix++;
                    if(isPartial) nFixPartial++;
                }

                //3) parte relativa a Nauth
                //numero di autori che hanno toccato una classe, ovviamente non si contano i duplicati, NauthTotal=il numero totale di autori diversi che hanno toccato quella classe fino a quel momento
                //NauthPartial=numero di autori diversi che in quella release hanno toccato la classe
                String authorEmail = commit.getAuthorIdent().getEmailAddress();
                nAuthTotal.add(authorEmail);
                if (isPartial) nAuthPartial.add(authorEmail);

            }

            metrics.setNrTotal(nr);
            metrics.setNrPartial(nrPartial);
            metrics.setnFixTotal(nFix);
            metrics.setnFixPartial(nFixPartial);
            metrics.setnAuthTotal(nAuthTotal.size());
            metrics.setnAuthPartial(nAuthPartial.size());

        } catch (Exception e) {
            System.err.println("Errore " + metrics.getClassName() + ": " + e.getMessage());
        }

    }

}
