package metrics;

import entity.ClassMetrics;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;

import java.util.HashSet;

import static metrics.Nfix.nFixTotal;

public class ComputeMetrics {

    public static void setMetricsTotal(ClassMetrics metrics, Git git, HashSet<String> buggyTicketList){

        int nr = 0;
        int nFix = 0;

        try {

            //head é l'identificativo del commit in cui mi trovo
            ObjectId head = git.getRepository().resolve("HEAD");


            Iterable<RevCommit> commits = git.log()//prendo tutti i commit relativi a quel file
                    .add(head)//parte da qui e va indietro
                    .addPath(metrics.getClassName())//prendo solo i commit che hanno toccato il file di interesse
                    .call();

            for (RevCommit commit : commits){

                //1) parte relativa a NR
                //conta il numero di volte che una classe é stata toccata da un commit relativo a un ticket di tipo BUG, total é relativo a tutta la vita della classe
                nr++;

                //2) parte relativa a Nfix
                String message = commit.getFullMessage();//recupero il commento del commit
                if (utils.metricsUtils.isCommitAFix(message, buggyTicketList)) {//controllo se nel messaggio c'é il riferimento a un ticket buggy
                    //System.out.println(message+filePath);
                    nFix++;
                }


            }

            metrics.setNRtotal(nr);
            metrics.setNfixTotal(nFix);

        } catch (Exception e) {
            System.err.println("Errore " + metrics.getClassName() + ": " + e.getMessage());
        }

    }

    public static void setMetricsPartial(ClassMetrics metrics, Git git, HashSet<String> buggyTicketList, String previousReleaseDate){

        int nr = 0;
        int nFix = 0;

        if (previousReleaseDate == null || previousReleaseDate.isEmpty()) {
           nFixTotal(metrics.getClassName(), buggyTicketList, git);
           return;
        }

        try {

            //head é l'identificativo del commit in cui mi trovo
            ObjectId head = git.getRepository().resolve("HEAD");


            Iterable<RevCommit> commits = git.log()
                    .add(head)
                    .addPath(metrics.getClassName())
                    .setRevFilter(CommitTimeRevFilter.after(utils.Miscellaneous.toDate(previousReleaseDate)))//controllo sulla data per non andare oltre la release precedente
                    .call();

            for (RevCommit commit : commits){

                //1) parte relativa a NR
                //conta il numero di volte che una classe é stata toccata da un commit relativo a un ticket di tipo BUG, total é relativo a tutta la vita della classe
                nr++;

                //2) parte relativa a Nfix
                String message = commit.getFullMessage();//recupero il commento del commit
                if (utils.metricsUtils.isCommitAFix(message, buggyTicketList)) {//controllo se nel messaggio c'é il riferimento a un ticket buggy
                    //System.out.println(message+filePath);
                    nFix++;
                }
            }

            metrics.setNRpartial(nr);
            metrics.setNfixPartial(nFix);

        } catch (Exception e) {
            System.err.println("Errore " + metrics.getClassName() + ": " + e.getMessage());
        }

    }


}
