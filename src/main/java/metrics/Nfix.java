package metrics;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Nfix {


    //conta il numero di volte che una classe é stata toccata da un commit relativo a un ticket di tipo BUG, total é relativo a tutta la vita della classe
    public static int nFixTotal(String filePath, HashSet<String> buggyTicketList, Git git){

        int nFix = 0;

        try {
            ObjectId head = git.getRepository().resolve("HEAD");

            //prendo tutti i commit relativi a quella classe
            Iterable<RevCommit> commits = git.log()
                    .add(head)
                    .addPath(filePath)
                    .call();

            for (RevCommit commit : commits) {
                //recupero il commento del commit
                String message = commit.getFullMessage();

                //controllo se nel messaggio c'é il riferimento a un ticket buggy
                if (isCommitAFix(message, buggyTicketList)) {
                    //System.out.println(message+filePath);
                    nFix++;
                }
            }

        } catch (Exception e) {
            System.err.println("err");
        }

        return nFix;
    }

    public static int nFixPartial(String filePath, HashSet<String> buggyTicketList, Git git, String previousReleaseDate){

        int nFix = 0;

        //caso prima release
        if (previousReleaseDate == null || previousReleaseDate.isEmpty()) {
            return nFixTotal(filePath, buggyTicketList, git);
        }

        try {
            ObjectId head = git.getRepository().resolve("HEAD");

            Iterable<RevCommit> commits = git.log()
                    .add(head)
                    .addPath(filePath)
                    .setRevFilter(CommitTimeRevFilter.after(utils.Miscellaneous.toDate(previousReleaseDate)))//controllo sulla data
                    .call();

            for (RevCommit commit : commits) {
                String message = commit.getFullMessage();
                if (isCommitAFix(message, buggyTicketList)) nFix++;
            }

        } catch (Exception e) {
            System.err.println("err");
        }

        return nFix;
    }



    private static boolean isCommitAFix(String comment, HashSet<String> bugTickets) {

        //definisco la regex per riconoscere nel testo la presenza di una stringa del formato OPENJPA-XXXX
        Pattern pattern = Pattern.compile("OPENJPA-\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(comment);

        while (matcher.find()) {
            String ticketFound = matcher.group(); // Es: "OPENJPA-1422"

            //verifico se il ticket si trova nella lista di quelli buggy
            if (bugTickets.contains(ticketFound)) return true;
        }
        return false;
    }


}
