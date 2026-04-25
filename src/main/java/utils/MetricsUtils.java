package utils;

import entity.LocChanges;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
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
import java.util.List;
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

    // conta le righe aggiunte e modificate in una classe tra padre e figlio, il file da confrontare deve essere messo del df
    public static int countLocAddedInClass(DiffFormatter df, RevCommit commit, Git git) {
        Repository repository = git.getRepository();
        int linesAdded = 0;

        try {
            List<DiffEntry> diffs;

            //il commit ha un parent
            if (commit.getParentCount() > 0) {
                //prendo il primo parent
                RevCommit parent = commit.getParent(0);
                if(parent.getTree() == null ) System.out.println("diocane");
                //confronta tra il padre e il figlio solo il file di interesse (lo passo al df a parte) e ottengo la lista dei cambiamenti
                diffs = df.scan(parent.getTree(), commit.getTree());
            } else {
                // caso in cui sia il primo commit del progetto, creo un albero vuoto per confrontarlo
                diffs = df.scan(new EmptyTreeIterator(), new CanonicalTreeParser(null, repository.newObjectReader(), commit.getTree()));
            }

            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = df.toFileHeader(diff);
                EditList edits = fileHeader.toEditList();

                // prendo sia le righe modificate che aggiunte
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

    // conta le righe aggiunte, modificate e rimosse in una classe tra padre e figlio, il file da confrontare deve essere messo del df
    public static int calculateChurnInClass(DiffFormatter df, RevCommit commit, Git git) {
        Repository repository = git.getRepository();
        int churn = 0;

        try {
            List<DiffEntry> diffs;

            if (commit.getParentCount() > 0) {
                RevCommit parent = commit.getParent(0);
                diffs = df.scan(parent.getTree(), commit.getTree());
            } else {
                // Caso primo commit
                diffs = df.scan(new EmptyTreeIterator(), new CanonicalTreeParser(null, repository.newObjectReader(), commit.getTree()));
            }

            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = df.toFileHeader(diff);
                EditList edits = fileHeader.toEditList();

                for (Edit edit : edits) churn += edit.getLengthA() + edit.getLengthB();
            }

        } catch (Exception e) {
            System.err.println("Errore nel calcolo del Churn al commit " + commit.getId().getName() + " : " + e.getMessage());
        }

        return churn;
    }
}
