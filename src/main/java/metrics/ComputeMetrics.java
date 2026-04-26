package metrics;

import entity.ClassMetrics;
import entity.LocChanges;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.MetricsUtils;

import java.util.*;

public class ComputeMetrics {

    private ComputeMetrics() {
        /* This utility class should not be instantiated */
    }

    public static void computeMetrics(ClassMetrics metrics, Git git, Set<String> buggyTicketList, ObjectId currentReleaseHash, ObjectId previousReleaseHash, String currentReleaseDate) {
        int nrTotal = 0, nrPartial = 0;
        int nFixTotal = 0, nFixPartial = 0;
        int locAddedTotal = 0, locAddedPartial = 0;
        int churnTotal = 0, churnPartial = 0;
        int maxChurnTotal = 0, maxChurnPartial = 0;
        int avgChurnTotal = 0, avgChurnPartial = 0;
        int age;
        int chgSetTotal = 0, chgSetPartial = 0;
        int maxChgSetTotal = 0, maxChgSetPartial = 0;
        int avgChgSetTotal = 0, avgChgSetPartial = 0;

        int oldestCommitTime = Integer.MAX_VALUE;

        HashSet<String> nAuthTotal = new HashSet<>();
        HashSet<String> nAuthPartial = new HashSet<>();

        try {
            // CALCOLO METRICHE PARZIALI
            Iterable<RevCommit> partialCommits;

            if (previousReleaseHash != null) {
                // uso il range per prendere correttamente i commit tra 2 release
                partialCommits = git.log().addRange(previousReleaseHash, currentReleaseHash).addPath(metrics.getFilePath()).call();
            } else {
                // Prima release: analizziamo tutti i commit fino alla release corrente
                partialCommits = git.log().add(currentReleaseHash).addPath(metrics.getFilePath()).call();
            }

            try (DiffFormatter df = MetricsUtils.createDiffFormatter(git, metrics.getFilePath())) {
                for (RevCommit commit : partialCommits) {
                    nrPartial++;// nrPartial
                    if (MetricsUtils.isCommitAFix(commit.getFullMessage(), buggyTicketList)) nFixPartial++;// nFixPartial
                    nAuthPartial.add(commit.getAuthorIdent().getEmailAddress());// nAuthPartial
                    // recupero le righe aggiunte, eliminate e modificate
                    LocChanges changes = MetricsUtils.getDetailedLocChanges(df, commit, git);
                    locAddedPartial += changes.getLocAdded();// locAddedPartial(added + modified)

                    // calcolo churn e maxChurn
                    int currentChurn = changes.getChurn();// churn(added + modified + deleted)
                    churnPartial += currentChurn;
                    if (currentChurn > maxChurnPartial) {
                        maxChurnPartial = currentChurn;
                    }

                    // Calcolo Change Set (Quanti file sono stati toccati INSIEME a questa classe in questo commit?)
                    int currentChangeSet = MetricsUtils.getChangeSetSize(commit, git);
                    chgSetPartial += currentChangeSet; // somma per la media

                    if (currentChangeSet > maxChgSetPartial) {
                        maxChgSetPartial = currentChangeSet;
                    }
                }
            }

            // CALCOLO METRICHE TOTALI
            Iterable<RevCommit> totalCommits = git.log().add(currentReleaseHash).addPath(metrics.getFilePath()).call();

            try (DiffFormatter df = MetricsUtils.createDiffFormatter(git, metrics.getFilePath())) {
                for (RevCommit commit : totalCommits) {
                    nrTotal++;// nrTotal
                    if (MetricsUtils.isCommitAFix(commit.getFullMessage(), buggyTicketList)) nFixTotal++;// nFixTotal
                    nAuthTotal.add(commit.getAuthorIdent().getEmailAddress());// nAuthTotal
                    LocChanges changes = MetricsUtils.getDetailedLocChanges(df, commit, git);
                    locAddedTotal += changes.getLocAdded();// locAddedTotal

                    // churn e maxChurn
                    int currentChurn = changes.getChurn();
                    churnTotal += currentChurn;
                    if (currentChurn > maxChurnTotal) {
                        maxChurnTotal = currentChurn;
                    }

                    // usato per calcolare l'age, il tempo viene calcolato in secondi da una certa data, quindi piu' il numero in secondi sara' piccolo piu la data sara' vecchia
                    int currentCommitTime = commit.getCommitTime();
                    if (currentCommitTime < oldestCommitTime) {
                        oldestCommitTime = currentCommitTime;
                    }

                    // calcolo changeSetSize total
                    int currentChangeSet = MetricsUtils.getChangeSetSize(commit, git);
                    chgSetTotal += currentChangeSet;

                    if (currentChangeSet > maxChgSetTotal) {
                        maxChgSetTotal = currentChangeSet;
                    }
                }
            }

            // calcolo l'age
            age = MetricsUtils.calculateAgeInDays(oldestCommitTime, currentReleaseHash, git, currentReleaseDate);

            // calcolo l'average churn, la variabile e' inizializzata a 0, quindi nel caso
            if (nrTotal > 0) avgChurnTotal = churnTotal/nrTotal;
            if (nrPartial > 0) avgChurnPartial = churnPartial / nrPartial;

            // calcolo l'average change set size
            if (nrTotal > 0) avgChgSetTotal = chgSetTotal / nrTotal;
            if (nrPartial > 0) avgChgSetPartial = chgSetPartial / nrPartial;


            // setto tutte le metriche calcolate
            metrics.setAllMetrics(nrTotal, nrPartial, nFixTotal, nFixPartial,
                    nAuthTotal.size(), nAuthPartial.size(),
                    locAddedTotal, locAddedPartial, churnTotal, churnPartial,
                    maxChurnTotal, maxChurnPartial, avgChurnTotal, avgChurnPartial,
                    age, chgSetTotal, chgSetPartial, maxChgSetTotal, maxChgSetPartial,
                    avgChgSetTotal, avgChgSetPartial);

        } catch (Exception e) {
            System.err.println("Errore nell'estrazione per " + metrics.getFilePath() + ": " + e.getMessage());
        }
    }

}
