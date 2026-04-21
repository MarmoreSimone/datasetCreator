package metrics;

import entity.ClassMetrics;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.MetricsUtils;

import java.util.HashSet;
import java.util.Set;

import static utils.MetricsUtils.createDiffFormatter;
import static utils.MetricsUtils.getLocAddedInCommit;

public class ComputeMetrics {

    private ComputeMetrics() {
        /* This utility class should not be instantiated */
    }

    public static void setMetrics(ClassMetrics metrics, Git git, Set<String> buggyTicketList, String previousReleaseDate) {

        long previousDateTime = 0;
        if (previousReleaseDate != null) previousDateTime = utils.Miscellaneous.toDate(previousReleaseDate).getTime();

        int nr = 0;
        int nrPartial = 0;
        int nFix = 0;
        int nFixPartial = 0;
        int locAddedTotal = 0;
        int locAddedPartial = 0;

        HashSet<String> nAuthTotal = new HashSet<>();
        HashSet<String> nAuthPartial = new HashSet<>();

        try {

            Repository repository = git.getRepository();
            ObjectId head = repository.resolve("HEAD");

            Iterable<RevCommit> commits = git.log()
                    .add(head)
                    .addPath(metrics.getClassName())
                    .call();

            try (DiffFormatter df = createDiffFormatter(repository, metrics.getClassName())) {

                for (RevCommit commit : commits) {

                    boolean isPartial = (commit.getCommitTime() * 1000L) > previousDateTime;

                    // Metrica NR
                    nr++;
                    if (isPartial) nrPartial++;

                    // Metrica NFix
                    if (MetricsUtils.isCommitAFix(commit.getFullMessage(), buggyTicketList)) {
                        nFix++;
                        if (isPartial) nFixPartial++;
                    }

                    // Metrica NAuth
                    String authorEmail = commit.getAuthorIdent().getEmailAddress();
                    nAuthTotal.add(authorEmail);
                    if (isPartial) nAuthPartial.add(authorEmail);

                    // Metrica locAdded
                    int linesAddedInCommit = getLocAddedInCommit(df, commit, repository);
                    locAddedTotal += linesAddedInCommit;
                    if (isPartial) locAddedPartial += linesAddedInCommit;
                }
            }

            metrics.setNrTotal(nr);
            metrics.setNrPartial(nrPartial);
            metrics.setnFixTotal(nFix);
            metrics.setnFixPartial(nFixPartial);
            metrics.setnAuthTotal(nAuthTotal.size());
            metrics.setnAuthPartial(nAuthPartial.size());
            metrics.setLocAddedTotal(locAddedTotal);
            metrics.setLocAddedPartial(locAddedPartial);

        } catch (Exception e) {
            System.err.println("Errore generale nell'estrazione per " + metrics.getClassName() + ": " + e.getMessage());
        }
    }

}
