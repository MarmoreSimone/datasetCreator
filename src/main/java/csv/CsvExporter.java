package csv;

import entity.ClassMetrics;
import entity.SmellInfo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    private CsvExporter(){}

    private static final String HEADER = "ReleaseID,version,ClassName,LOC,NRtotal,NRpartial,NfixTotal,NfixPartial,NauthTotal,NauthPartial,locAddedTotal,locAddedPartial,churnTotal,ChurnPartial,maxChurnTotal,maxChurnPartial,avgChurnTotal,avgChurnPartial,age,chgSetTotal,chgSetPartial,maxChangeSetTotal,maxChangeSetPartial,avgChgSetTotal,avgChgSetPartial,smells,Buggy";
    private static final String HEADER_M4 = "classPath,ruleName,priority,description,startLine,endLine";

    public static void exportToCsv(List<ClassMetrics> classMetrics, String outputDatasetPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDatasetPath))) {

            //header
            writer.write(HEADER);
            writer.newLine();

            //righe
            for (ClassMetrics metrics : classMetrics) {
                writer.write(metrics.toCsvRow());
                writer.newLine();
            }

            System.out.println("righe dataset: " + classMetrics.size());

        } catch (IOException e) {
            System.err.println("errore durante la scrittura del CSV: " + e.getMessage());
        }
    }

    // usata per milestone 4, esporta il csv con dentro le info degli smell
    public static void exportToCsvM4(List<SmellInfo> classMetrics, String outputDatasetPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDatasetPath))) {

            //header
            writer.write(HEADER_M4);
            writer.newLine();

            //righe
            for (SmellInfo smellInfo : classMetrics) {
                writer.write(smellInfo.toCsvRow());
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("errore durante la scrittura del CSV: " + e.getMessage());
        }
    }
}