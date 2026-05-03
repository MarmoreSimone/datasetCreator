package metrics;

import entity.ClassMetrics;
import entity.LocChanges;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.MetricsUtils;

import java.util.HashSet;
import java.util.Set;

public class ComputeMetrics {

    private ComputeMetrics() {
        /* This utility class should not be instantiated */
    }

    public static void computeMetrics(ClassMetrics metrics, Git git, Set<String> buggyTicketList, ObjectId currentReleaseHash, ObjectId previousReleaseHash, String currentReleaseDate) {
        try {
            Iterable<RevCommit> partialCommits;
            if (previousReleaseHash != null) {
                // prendo solo i commit relativi alla release i-esima
                partialCommits = git.log().addRange(previousReleaseHash, currentReleaseHash).addPath(metrics.getFilePath()).call();
            } else {
                // caso in cui siamo nella prima release
                partialCommits = git.log().add(currentReleaseHash).addPath(metrics.getFilePath()).call();
            }

            // prendo tutti i commit dalla prima release ad ora
            Iterable<RevCommit> totalCommits = git.log().add(currentReleaseHash).addPath(metrics.getFilePath()).call();

            // calcolo delle metriche (true per i partial, false per i total)
            extractAndSetMetrics(partialCommits, git, metrics, buggyTicketList, currentReleaseDate, true);
            extractAndSetMetrics(totalCommits, git, metrics, buggyTicketList, currentReleaseDate, false);

        } catch (Exception e) {
            System.err.println("Errore nell'estrazione per " + metrics.getFilePath() + ": " + e.getMessage());
        }
    }

    private static void extractAndSetMetrics(Iterable<RevCommit> commits, Git git, ClassMetrics metrics, Set<String> buggyTicketList, String currentReleaseDate, boolean isPartial) {
        int nr = 0;
        int nFix = 0;
        int locAdded = 0;
        int churn = 0;
        int maxChurn = 0;
        int chgSet = 0;
        int maxChgSet = 0;
        int oldestCommitTime = Integer.MAX_VALUE;
        Set<String> nAuth = new HashSet<>();

        try (DiffFormatter df = MetricsUtils.createDiffFormatter(git, metrics.getFilePath())) {
            for (RevCommit commit : commits) {
                nr++;

                if (MetricsUtils.isCommitAFix(commit.getFullMessage(), buggyTicketList)) {
                    nFix++;
                }

                nAuth.add(commit.getAuthorIdent().getEmailAddress());

                // recupero righe aggiunte eliminate e modificate
                LocChanges changes = MetricsUtils.getDetailedLocChanges(df, commit, git);
                locAdded += changes.getLocAdded();// aggiunte + modificate

                int currentChurn = changes.getChurn();// aggiunte + modificate + eliminate
                churn += currentChurn;
                if (currentChurn > maxChurn) {
                    maxChurn = currentChurn;
                }

                int currentCommitTime = commit.getCommitTime();
                if (currentCommitTime < oldestCommitTime) {
                    oldestCommitTime = currentCommitTime;
                }

                int currentChangeSet = MetricsUtils.getChangeSetSize(commit, git);
                chgSet += currentChangeSet;
                if (currentChangeSet > maxChgSet) {
                    maxChgSet = currentChangeSet;
                }
            }
        }

        // calcolo medie
        int avgChurn = 0;
        int avgChgSet = 0;
        if (nr > 0) {
            avgChurn = churn / nr;
            avgChgSet = chgSet / nr;
        }

        if (isPartial) {
            metrics.setNrPartial(nr);
            metrics.setnFixPartial(nFix);
            metrics.setnAuthPartial(nAuth.size());
            metrics.setLocAddedPartial(locAdded);
            metrics.setChurnPartial(churn);
            metrics.setMaxChurnPartial(maxChurn);
            metrics.setAvgChurnPartial(avgChurn);
            metrics.setChgSetPartial(chgSet);
            metrics.setMaxChgSetPartial(maxChgSet);
            metrics.setAvgChgSetPartial(avgChgSet);
        } else {
            metrics.setNrTotal(nr);
            metrics.setnFixTotal(nFix);
            metrics.setnAuthTotal(nAuth.size());
            metrics.setLocAddedTotal(locAdded);
            metrics.setChurnTotal(churn);
            metrics.setMaxChurnTotal(maxChurn);
            metrics.setAvgChurnTotal(avgChurn);
            metrics.setChgSetTotal(chgSet);
            metrics.setMaxChgSetTotal(maxChgSet);
            metrics.setAvgChgSetTotal(avgChgSet);
            metrics.setAge(MetricsUtils.calculateAgeInDays(oldestCommitTime, currentReleaseDate));// age calcolata solo sui totali
        }
    }
}
