package utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.StreamSupport;

public class GitUtils {

    //Inizializza il repository Git
    public static Git openRepository(String repoPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoPath, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        return new Git(repository);
    }

    public static void checkoutToDate(Git git, String dataRelease) {
        try {
            // 1. Data target in secondi (JGit usa i secondi, non i millisecondi)
            long targetTimeSec = java.sql.Date.valueOf(dataRelease).getTime() / 1000L;

            // 2. Trova il commit migliore usando gli Stream
            RevCommit bestCommit = StreamSupport.stream(git.log().all().call().spliterator(), false)
                    .filter(c -> c.getCommitTime() <= targetTimeSec) // Solo quelli precedenti alla release
                    .max(Comparator.comparingInt(RevCommit::getCommitTime)) // Prende il più recente tra quelli filtrati
                    .orElse(null);

            // 3. Eseguiamo il reset
            if (bestCommit != null) {
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(bestCommit.getName()).call();

                java.util.Date dataCommitReale = new java.util.Date(bestCommit.getCommitTime() * 1000L);
                System.out.printf("   [GIT] Allineato al commit: %s del %s%n",
                        bestCommit.getName().substring(0, 7), dataCommitReale);
            } else {
                System.out.println("   [GIT] ATTENZIONE: Nessun commit trovato prima del " + dataRelease);
            }

        } catch (Exception e) {
            System.err.println("Errore nel checkout temporale: " + e.getMessage());
        }
    }

}