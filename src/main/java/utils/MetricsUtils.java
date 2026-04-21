package utils;

import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsUtils {

    private MetricsUtils(){}

    public static boolean isCommitAFix(String comment, Set<String> bugTickets) {

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

    public static int getLocAddedInCommit(DiffFormatter df, RevCommit commit, Repository repository) {
        int linesAdded = 0;

        try {
            List<DiffEntry> diffs;

            if (commit.getParentCount() > 0) {
                // Ha un parent
                RevCommit parent = commit.getParent(0);
                diffs = df.scan(parent.getTree(), commit.getTree());
            } else {
                // Primo commit assoluto della storia (Orphan commit)
                diffs = df.scan(new EmptyTreeIterator(),
                        new CanonicalTreeParser(null, repository.newObjectReader(), commit.getTree()));
            }

            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = df.toFileHeader(diff);
                EditList edits = fileHeader.toEditList();

                for (Edit edit : edits) {
                    if (edit.getType() == Edit.Type.INSERT || edit.getType() == Edit.Type.REPLACE) {
                        linesAdded += edit.getLengthB();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel calcolo del Diff al commit " + commit.getId().getName() + " : " + e.getMessage());
        }

        return linesAdded;
    }

    public static DiffFormatter createDiffFormatter(Repository repository, String className) {

        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(repository);
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);
        df.setPathFilter(PathFilter.create(className));
        return df;

    }

}
