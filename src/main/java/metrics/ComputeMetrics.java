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

    public static void computeMetrics(ClassMetrics metrics, Git git, Set<String> buggyTicketList, ObjectId currentReleaseHash, ObjectId previousReleaseHash) {
        int nrTotal = 0, nrPartial = 0;
        int nFixTotal = 0, nFixPartial = 0;
        int locAddedTotal = 0, locAddedPartial = 0;
        int churnTotal = 0, churnPartial = 0;
        int maxChurnTotal = 0, maxChurnPartial = 0;
        int avgChurnTotal = 0, avgChurnPartial = 0;

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
                    int currentChurn = changes.getChurn();
                    churnPartial += currentChurn;
                    if (currentChurn > maxChurnPartial) {
                        maxChurnPartial = currentChurn;
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
                }
            }

            metrics.setNrTotal(nrTotal);
            metrics.setNrPartial(nrPartial);

            metrics.setnFixTotal(nFixTotal);
            metrics.setnFixPartial(nFixPartial);

            metrics.setnAuthTotal(nAuthTotal.size());
            metrics.setnAuthPartial(nAuthPartial.size());

            metrics.setLocAddedTotal(locAddedTotal);
            metrics.setLocAddedPartial(locAddedPartial);

            metrics.setChurnTotal(churnTotal);
            metrics.setChurnPartial(churnPartial);

            metrics.setMaxChurnTotal(maxChurnTotal);
            metrics.setMaxChurnPartial(maxChurnPartial);

            if (nrTotal > 0) metrics.setAvgChurnTotal(churnTotal / nrTotal);
            else metrics.setAvgChurnTotal(0);

            if (nrPartial > 0) metrics.setAvgChurnPartial(churnPartial / nrPartial);
            else metrics.setAvgChurnPartial(0);


        } catch (Exception e) {
            System.err.println("Errore nell'estrazione per " + metrics.getFilePath() + ": " + e.getMessage());
        }
    }
}
