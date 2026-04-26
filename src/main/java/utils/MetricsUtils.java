package utils;

import entity.LocChanges;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    public static LocChanges getDetailedLocChanges(DiffFormatter df, RevCommit commit, Git git) {
        Repository repository = git.getRepository();
        int added = 0;
        int deleted = 0;
        int modified = 0;

        try {
            List<DiffEntry> diffs;

            if (commit.getParentCount() > 0) {

                RevCommit parent = commit.getParent(0);
                diffs = df.scan(parent.getTree(), commit.getTree());
            } else {
                // Caso del primo commit assoluto del progetto
                diffs = df.scan(new EmptyTreeIterator(), new CanonicalTreeParser(null, repository.newObjectReader(), commit.getTree()));
            }

            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = df.toFileHeader(diff);
                org.eclipse.jgit.diff.EditList edits = fileHeader.toEditList();

                for (org.eclipse.jgit.diff.Edit edit : edits) {
                    int a = edit.getLengthA(); // Righe nel vecchio commit (Lato A)
                    int b = edit.getLengthB(); // Righe nel nuovo commit (Lato B)

                    switch (edit.getType()) {
                        case INSERT:
                            added += b;
                            break;
                        case DELETE:
                            deleted += a;
                            break;
                        case REPLACE:
                            // Calcolo MSR di precisione per le modifiche
                            int mod = Math.min(a, b);
                            modified += mod;
                            added += (b - mod);
                            deleted += (a - mod);
                            break;
                        case EMPTY:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel calcolo del Diff al commit " + commit.getId().getName() + " : " + e.getMessage());
        }

            return new LocChanges(added,deleted,modified);
    }

    public static DiffFormatter createDiffFormatter(Git git, String className) {
        Repository repository = git.getRepository();
        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(repository);
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);
        //scarto tutto tranne la classe che mi interessa
        df.setPathFilter(PathFilter.create(className));
        return df;
    }

    // funzione che conta le righe in un file, considera tutto
    public static int countLocInClass(String repoPath, String filePath) {
        File file = new File(repoPath, filePath);
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // lines() crea uno stream di righe, count() le conta.
            // Poiché count() restituisce un 'long', facciamo un cast a (int)
            return (int) reader.lines().count();
        } catch (IOException _) {
            System.err.println("Errore durante la lettura del file per contare le LOC: " + file.getPath());
            return 0; // Se c'è un errore, restituiamo 0
        }
    }

    // dato il path della repo trova tutti i path delle classi .java esclusi i test
    public static List<String> getJavaFilePaths(String repoPath) throws IOException {
        Path projectRoot = Paths.get(repoPath);

        try (Stream<Path> walk = Files.walk(projectRoot)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    // Escludiamo i test ma teniamo tutto il resto della struttura
                    .filter(p -> !p.toString().contains("/test/") && !p.toString().contains("\\test\\"))
                    // prendiamo il percorso dalla root del progetto in poi
                    .map(p -> convertPath(projectRoot.relativize(p).toString()))//mantengo solo la parte relativa al file del progetto
                    .toList();
        }
    }

    //USATO PER WINDOWS
    private static String convertPath(String filePath) {

        String convertedPath = filePath.replace("\\", "/");
        if (convertedPath.startsWith("/")) {
            convertedPath = convertedPath.substring(1);
        }

        return convertedPath;
    }

    public static int calculateAgeInDays(int oldestCommitTime, ObjectId currentReleaseHash, Git git, String currentReleaseDate) {
        // se la variabile non è mai stata aggiornata, significa che la classe non ha commit
        if (oldestCommitTime == Integer.MAX_VALUE) {
            return 0;
        }

        try {
            // leggo la stringa passata dal parametro (es. "2007-04-15") e la converto in date
            LocalDate parsedDate = LocalDate.parse(currentReleaseDate);

            // converto la data in secondi
            int releaseTime = (int) parsedDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();

            // calcolo la differenza di tempo
            int ageInSeconds = releaseTime - oldestCommitTime;

            // protezione da eventuali "viaggi nel tempo"
            if (ageInSeconds < 0) {
                System.err.println("viaggio nel tempo");
                return 0;
            }

            // converto i secondi in giorni
            return ageInSeconds / (60 * 60 * 24);

        } catch (Exception e) {
            System.err.println("Errore nel calcolo dell'Age tramite stringa: " + e.getMessage());
            return 0;
        }
    }

    // mi dice il numero di altre classi che sono state toccate oltre a questa in un commit
    public static int getChangeSetSize(RevCommit commit, Git git) {
        int changeSetSize = 0;

        // creo un formatter pulito senza PathFilter cosi da poter prendere tutte le classi
        try (DiffFormatter df = new DiffFormatter(org.eclipse.jgit.util.io.DisabledOutputStream.INSTANCE)) {
            df.setRepository(git.getRepository());
            df.setDiffComparator(org.eclipse.jgit.diff.RawTextComparator.DEFAULT);
            df.setDetectRenames(true);

            List<DiffEntry> diffs;

            if (commit.getParentCount() > 0) {
                try (org.eclipse.jgit.revwalk.RevWalk rw = new org.eclipse.jgit.revwalk.RevWalk(git.getRepository())) {
                    RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
                    diffs = df.scan(parent.getTree(), commit.getTree());
                }
            } else {
                // Primo commit
                diffs = df.scan(new org.eclipse.jgit.treewalk.EmptyTreeIterator(),
                        new org.eclipse.jgit.treewalk.CanonicalTreeParser(null, git.getRepository().newObjectReader(), commit.getTree()));
            }

            // La grandezza della lista è esattamente il numero di file toccati indipendentemente dal tipo di modifica
            changeSetSize = diffs.size();

        } catch (Exception e) {
            System.err.println("Errore nel calcolo del Change Set Size: " + e.getMessage());
        }

        return changeSetSize;
    }

    /**
     * Esegue PMD su tutta la repository in un'unica passata e restituisce
     * una mappa <PercorsoRelativo, NumeroDiSmell>
     */
    public static Map<String, Integer> getSmells(String repoPath) {
        Map<String, Integer> smellsMap = new HashMap<>();
        PMDConfiguration config = new PMDConfiguration();

        config.setDefaultLanguageVersion(LanguageRegistry.PMD.getLanguageById("java").getDefaultVersion());

        // Regole mirate per la Defect Prediction
        config.addRuleSet("category/java/design.xml");
        config.addRuleSet("category/java/errorprone.xml");

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            // Aggiungiamo l'intera directory. PMD troverà automaticamente tutti i .java
            pmd.files().addDirectory(Paths.get(repoPath));

            Report report = pmd.performAnalysisAndCollectReport();

            // Iteriamo sulle violazioni direttamente come oggetti Java
            for (RuleViolation violation : report.getViolations()) {
                String fullPath = violation.getFileId().getAbsolutePath();

                // Puliamo il path per renderlo identico a quello che usi nel dataset
                String relativePath = cleanPath(fullPath, repoPath);

                smellsMap.put(relativePath, smellsMap.getOrDefault(relativePath, 0) + 1);
            }
        } catch (Exception e) {
            System.err.println("Errore durante l'analisi PMD batch: " + e.getMessage());
        }

        return smellsMap;
    }

    private static String cleanPath(String fullPath, String repoPath) {
        // Uniformiamo i separatori per evitare bug tra Windows e Linux
        String cleanFullPath = fullPath.replace("\\", "/");
        String cleanRepoPath = repoPath.replace("\\", "/");

        int index = cleanFullPath.indexOf(cleanRepoPath);
        if (index != -1) {
            // +1 per rimuovere lo slash iniziale (es: da "/src/..." a "src/...")
            return cleanFullPath.substring(index + cleanRepoPath.length() + 1);
        }
        return fullPath;
    }
}
