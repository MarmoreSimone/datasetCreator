package metrics;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

public class NRtotal {

    public static int NRtotal(Git git, String relativeFilePath, String dataRelease) {
        int nr = 0;
        try {
            String gitPath = relativeFilePath.replace("\\", "/");
            if (gitPath.startsWith("/")) gitPath = gitPath.substring(1);

            // DEBUG 1: Vediamo se il repository è aperto correttamente
            // System.out.println("Analizzando: " + git.getRepository().getDirectory());

            // DEBUG 2: Proviamo a chiedere i commit SENZA filtri di percorso per vedere se ne trova almeno uno
            Iterable<RevCommit> allCommits = git.log().setMaxCount(1).call();
            if (!allCommits.iterator().hasNext()) {
                System.err.println("ERRORE: JGit non vede nessun commit nel repository!");
                return -1;
            }

            // Chiamata standard
            Iterable<RevCommit> commits = git.log()
                    .add(git.getRepository().resolve("HEAD"))
                    .addPath(gitPath)
                    .call();

            for (RevCommit commit : commits) {
                nr++;
                // System.out.println("Commit trovato per " + gitPath + ": " + commit.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return nr;
    }


}
