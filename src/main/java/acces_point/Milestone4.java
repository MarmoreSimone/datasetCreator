package acces_point;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import csv.CsvExporter;
import entity.ClassMetrics;
import entity.SmellInfo;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.eclipse.jgit.api.Git;
import utils.GitUtils;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static utils.MetricsUtils.*;

public class Milestone4 {

    private static final String REPO_OPENJPA_PATH = "openjpa";
    private static final String OUTPUT_SMELLS_INFO_PATH1 = "src/main/java/outputs/smell_classe_1.csv";
    private static final String OUTPUT_SMELLS_INFO_PATH2 = "src/main/java/outputs/smell_classe_2.csv";

    public static void main() {
        try {

            Git git = GitUtils.openRepository(REPO_OPENJPA_PATH);
            GitUtils.checkoutToTag(git, "4.1.1");// ultima release
            List<String> classPaths = getJavaFilePaths(REPO_OPENJPA_PATH);
            Map<String, Integer> smellMap = getSmells(REPO_OPENJPA_PATH);
            List<ClassMetrics> classes = new ArrayList<>();

            classPaths.forEach(filePath -> {
                try {

                    File sourceFile = new File(REPO_OPENJPA_PATH, filePath);
                    CompilationUnit cu = StaticJavaParser.parse(sourceFile);

                    // tolgo la directory che contiene gli esempi
                    if (filePath.contains("openjpa-examples")) return;
                    int loc = countLocInClass(REPO_OPENJPA_PATH, filePath);
                    int smell = smellMap.getOrDefault(filePath, 0);
                    int nMethods = getNumberOfMethods(cu);

                    classes.add(new ClassMetrics(filePath, loc, nMethods,smell));

                } catch (Exception e) {
                    System.err.println("impossibile analizzare il file: " + filePath + " - " + e.getMessage());
                }
            });

            // ripristino il repository
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("master").call();

            double[] locValues = classes.stream().mapToDouble(ClassMetrics::getLoc).toArray();
            double[] smellValues = classes.stream().mapToDouble(ClassMetrics::getSmells).toArray();
            double[] nMethodValues = classes.stream().mapToDouble(ClassMetrics::getnMethods).toArray();

            // Istanziamo il calcolatore una sola volta
            Percentile percentile = new Percentile();

            // Calcoliamo le soglie
            double locThreshold = percentile.evaluate(locValues, 90.0);
            double smellThreshold = percentile.evaluate(smellValues, 90.0);
            double nMethodThreshold = percentile.evaluate(nMethodValues, 90.0);

            // filtraggio e ordinamento delle classi
            List<ClassMetrics> rankedClasses = classes.stream()
                    // tolgo classi con LOC <= threshold e con meno di 3 metodi e smells <= threshold
                    .filter(c -> c.getLoc() > locThreshold && c.getnMethods() >= 3 && c.getSmells() > smellThreshold)
                    // ordino per nSmells in modo decrescente
                    .sorted(Comparator.comparingInt(ClassMetrics::getSmells).reversed())
                    .toList();

            // seleziono le due classi (prima e ultima della lista, considerando la roba del modulo con la lettera del nome(viene 4))
            if (rankedClasses.size() >= 2) {
                ClassMetrics firstClass = rankedClasses.get(0 + 4); // prima classe
                ClassMetrics lastClass = rankedClasses.get(rankedClasses.size() - (1 + 4)); // ultima classe

                System.out.println("PRIMA CLASSE");
                System.out.printf("Path: %s | Smells: %d | Metodi: %d | LOC: %d%n%n", firstClass.getFilePath(), firstClass.getSmells(), firstClass.getnMethods(), firstClass.getLoc());
                getSmellDetails(REPO_OPENJPA_PATH + File.separator + firstClass.getFilePath(),OUTPUT_SMELLS_INFO_PATH1);
                System.out.println("ULTIMA CLASSE");
                System.out.printf("Path: %s | Smells: %d | Metodi: %d | LOC: %d%n", lastClass.getFilePath(), lastClass.getSmells(), lastClass.getnMethods(), lastClass.getLoc());
                getSmellDetails(REPO_OPENJPA_PATH + File.separator + lastClass.getFilePath(),OUTPUT_SMELLS_INFO_PATH2);

            } else {
                System.err.println("non ci sono abbastanza classi dopo il filtraggio per effettuare la selezione.");
            }
        } catch (Exception e) {
            System.err.println("errore milestone 4: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int getNumberOfMethods(CompilationUnit cu) {
        if (cu == null) return 0;
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        return methods.size();
    }

    // crea il csv che contiene gli smell con le info
    public static void getSmellDetails(String filePath, String outputFilePath) {
        PMDConfiguration config = new PMDConfiguration();

        config.setDefaultLanguageVersion(LanguageRegistry.PMD.getLanguageById("java").getDefaultVersion());

        config.addRuleSet("category/java/design.xml");
        config.addRuleSet("category/java/errorprone.xml");

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            pmd.files().addFile(Paths.get(filePath));
            Report report = pmd.performAnalysisAndCollectReport();
            List<RuleViolation> violations = report.getViolations();

            // si poteva usare direttamente la classe RuleViolation
            List<SmellInfo> smellList = new ArrayList<>();
            for (RuleViolation v : violations) {
                smellList.add(new SmellInfo(filePath,v.getRule().getName(),v.getRule().getPriority().getPriority(),v.getDescription(),v.getBeginLine(),v.getEndLine()));
            }
            CsvExporter.exportToCsvM4(smellList,outputFilePath);

        } catch (Exception e) {
            System.err.println("errore durante l'analisi PMD: " + e.getMessage());
        }
    }

}
