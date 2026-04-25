import entity.ReleaseInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.GitUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ReleaseLogicalPredecessorTest {

    private List<ReleaseInfo> releases;

    @BeforeEach
    void setUp() {
        // Inizializziamo la lista rispettando il tuo esatto ordine cronologico
        String[] versionStrings = {
                "0.9.0", "0.9.6", "0.9.7", "1.0.0", "1.0.1", "1.0.2", "1.1.0",
                "1.0.3", "1.2.0", "2.0.0-M1", "1.2.1", "2.0.0-M2", "2.0.0-M3",
                "1.2.2", "2.0.0-beta", "2.0.0-beta2", "2.0.0-beta3", "2.0.0",
                "2.0.1", "1.0.4", "2.1.0", "2.1.1", "2.2.0", "2.2.1", "1.2.3",
                "2.2.2", "2.3.0", "2.4.0", "2.4.1", "2.4.2", "3.0.0", "3.1.0",
                "3.1.1", "3.1.2", "3.2.0", "3.2.1", "3.2.2", "4.0.0", "4.0.1",
                "4.1.0", "4.1.1"
        };

        // Usiamo IntStream per scorrere gli indici dell'array
        releases = java.util.stream.IntStream.range(0, versionStrings.length)
                .mapToObj(i -> new ReleaseInfo(
                        String.valueOf(i + 1),    // releaseID fittizio (es: "1", "2", "3"...)
                        versionStrings[i],        // releaseName reale dall'array
                        "2024-01-01"              // data fittizia (il tuo test non la usa)
                ))
                .collect(Collectors.toList());
    }

    @Test
    void testStandardAdvancement() {
        // Target: 1.2.0 (Indice 8)
        // La release precedente logica e cronologica è 1.1.0
        ReleaseInfo result = GitUtils.findLogicalPredecessor(releases, 8);

        assertEquals("1.1.0", result.getReleaseID(),
                "Il predecessore di un avanzamento lineare dovrebbe essere la minor precedente");
    }

    @Test
    void testIntermediateBackport() {
        // Target: 1.0.3 (Indice 7)
        // È uscita DOPO la 1.1.0, ma il suo genitore logico è la 1.0.2
        ReleaseInfo result = GitUtils.findLogicalPredecessor(releases, 7);

        assertEquals("1.0.2", result.getReleaseID(),
                "Una patch rilasciata in ritardo deve ignorare le minor/major successive");
    }

    @Test
    void testDeepBackport() {
        // Target: 1.0.4 (Indice 19)
        // È uscita molto tardi, dopo le 2.0.x. Il genitore logico è la 1.0.3 trovata all'indice 7
        ReleaseInfo result = GitUtils.findLogicalPredecessor(releases, 19);

        assertEquals("1.0.3", result.getReleaseID(),
                "Un backport molto profondo deve ritrovare la patch corretta saltando tutte le versioni superiori");
    }

    @Test
    void testMilestoneAdvancement() {
        // Target: 2.0.0-beta (Indice 14)
        // Il predecessore logico diretto tra le versioni precedenti è la 2.0.0-M3
        ReleaseInfo result = GitUtils.findLogicalPredecessor(releases, 14);

        assertEquals("2.0.0-M3", result.getReleaseID(),
                "Le versioni di pre-release devono rispettare la gerarchia dei qualificatori (M3 < beta)");
    }

    @Test
    void testFirstReleaseHasNoPredecessor() {
        // Target: 0.9.0 (Indice 0)
        // Essendo la prima in assoluto (i = 0), il ciclo for non parte nemmeno
        ReleaseInfo result = GitUtils.findLogicalPredecessor(releases, 0);

        assertNull(result, "La prima release in assoluto non deve avere predecessori");
    }
}