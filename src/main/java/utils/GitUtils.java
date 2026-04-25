package utils;

import entity.ReleaseInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GitUtils {

    // mappa che contiene il commit per i tag che non sono esplicitati nel progetto
    private static final Map<String, String> MANUAL_VERSIONS = new HashMap<>();
    static {
        MANUAL_VERSIONS.put("0.9.0", "dc1f0bf2046cf08d9484a287dee02ff058961ca4");
        MANUAL_VERSIONS.put("2.0.0-M1", "e80bac22d0a641f8d5812456365e241aa68a5aae");
        MANUAL_VERSIONS.put("2.0.0-M2", "19be3fabf1c62796e4c9089ed3ebd6ccf4f418ca");
    }

    private GitUtils(){}

    // inizializzo il repository Git
    public static Git openRepository(String repoPath) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoPath, ".git")).readEnvironment().findGitDir().build();

        Git git = new Git(repository);
        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("master").call();

        return git;
    }

    // funzione che recupera un objectId dato un tag, quindi gli passi 1.2.1 e lui ti ritorna l'objectid contente l'hash del commit
    public static ObjectId getObjectIdFromTag(Git git, String tagName) throws Exception {
        Repository repository = git.getRepository();
        ObjectId commitId;

        // caso in cui il tag non si trova nel progetto, lo recupero da quelli inseriti manualmente
        if (MANUAL_VERSIONS.containsKey(tagName)) {
            commitId = repository.resolve(MANUAL_VERSIONS.get(tagName));
        } else {
            // ricostruisco il percorso completo
            String tagRefName = "refs/tags/" + tagName;

            // provo con tutto il percorso
            Ref tagRef = repository.findRef(tagRefName);

            // provo solo con il tag
            if (tagRef == null) tagRef = repository.findRef(tagName);
            if (tagRef == null) throw new Exception("Tag non trovato: " + tagName);

            //estraggo il commit dal tag
            Ref peeledRef = repository.getRefDatabase().peel(tagRef);
            commitId = peeledRef.getPeeledObjectId();
            if (commitId == null) commitId = tagRef.getObjectId();
        }

        return commitId;
    }

    public static RevCommit checkoutToTag(Git git, String tagName) throws Exception {
        Repository repository = git.getRepository();

        // faccio pulizia di file indesiderati
        git.reset().setMode(ResetCommand.ResetType.HARD).call();
        git.clean().setCleanDirectories(true).setForce(true).call();

        // recupero il commit su cui posizionarmi dal tag
        ObjectId commitId = getObjectIdFromTag(git, tagName);

        // checkout dei file
        try {
            git.checkout().setName(commitId.getName()).setForce(true).setCreateBranch(false).call();
        } catch (CheckoutConflictException e) {
            // uso le cattive
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commitId.getName()).call();
        }

        // posiziono il RevWalk e ritorno il commit con tutte le sue belle informazioni
        try (RevWalk walk = new RevWalk(repository)) {
            return walk.parseCommit(commitId);
        }
    }

    // funzione per recuperare i tag interni al progetto
    public static List<String> getAllGitTags(Git git) throws Exception {
        List<String> tagNames = new ArrayList<>();

        // recupero la lista di tutti i riferimenti (Ref) dei tag
        List<Ref> call = git.tagList().call();

        for (Ref ref : call) {
            // ref.getName() restituisce stringhe come "refs/tags/1.2.3" pulisco la stringa tenendo solo il nome finale
            String cleanName = ref.getName().replace("refs/tags/", "");
            tagNames.add(cleanName);
        }

        return tagNames;
    }

    // funzione che prende il tag dal csv ricavato dal codice del falessi e lo trasforma nel formato esatto con cui é salvato nel progetto
    public static String findMatchingTag(String jiraVersion, List<String> allGitTags) {
        if (MANUAL_VERSIONS.containsKey(jiraVersion)) {
            System.out.println("commit manuale per: " + jiraVersion);
            return jiraVersion;
        }

        // jira dice "1.2.3" e Git ha "1.2.3"
        if (allGitTags.contains(jiraVersion)) return jiraVersion;


        // jira ha "0.9.6" ma Git ha "0.9.6-incubating"
        for (String gitTag : allGitTags) {
            if (gitTag.startsWith(jiraVersion + "-incubating")) {
                return gitTag;
            }
        }

        // jira dice "2.3.0" ma su Git c'è solo "openjpa-parent-2.3.0"
        if (allGitTags.contains("openjpa-parent-" + jiraVersion)) return "openjpa-parent-" + jiraVersion;

        System.out.println("nessun match per: " + jiraVersion);
        return null;
    }

    public static ReleaseInfo findLogicalPredecessor(List<ReleaseInfo> releases, int currentIndex) {
        ReleaseInfo currentRelease = releases.get(currentIndex);

        // modifico il nome della versione per far funzionare correttamente il comparatore in modo che metta bene M -> beta -> 2.0.0
        String normalizedTarget = modifyVersionName(currentRelease.getReleaseID());
        ComparableVersion targetVersion = new ComparableVersion(normalizedTarget);

        ReleaseInfo bestPredecessor = null;
        ComparableVersion bestPredecessorVersion = null;

        for (int i = 0; i < currentIndex; i++) {
            ReleaseInfo candidate = releases.get(i);

            // modifico la versione candidata
            String normalizedCandidate = modifyVersionName(candidate.getReleaseID());
            ComparableVersion candidateVersion = new ComparableVersion(normalizedCandidate);

            if (candidateVersion.compareTo(targetVersion) < 0) {
                if (bestPredecessorVersion == null || candidateVersion.compareTo(bestPredecessorVersion) > 0) {
                    bestPredecessorVersion = candidateVersion;
                    bestPredecessor = candidate;
                }
            }
        }
        return bestPredecessor;
    }

    private static String modifyVersionName(String version) {
        if (version == null) return null;
        // sostituisce "-M" seguito da numeri con "-alpha".
        // "2.0.0-M3" diventa "2.0.0-alpha3"
        return version.replaceAll("-M(\\d+)", "-alpha$1");
    }
}