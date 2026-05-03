package acces_point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RetrieveTicketsID {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream();
             BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    static void main() throws IOException, JSONException {

        String projName = "OPENJPA";
        Integer j = 0;
        Integer i = 0;
        Integer total = 1;
        String outputFile = "jiraTicketsEnriched.csv";

        System.out.println("Inizio download ticket Jira per il progetto " + projName + "...");

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Scriviamo l'intestazione del CSV
            writer.println("TicketID,CreationDate,ResolutionDate,AffectedVersions");

            do {
                // Pagina di 1000 risultati alla volta
                j = i + 1000;
                String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                        + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                        + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                        + i.toString() + "&maxResults=" + j.toString();

                JSONObject json = readJsonFromUrl(url);
                JSONArray issues = json.getJSONArray("issues");
                total = json.getInt("total");

                for (; i < total && i < j; i++) {
                    // Prendi l'oggetto ticket corrente
                    JSONObject issue = issues.getJSONObject(i % 1000);
                    String key = issue.getString("key");

                    JSONObject fields = issue.getJSONObject("fields");

                    // 1. Data Creazione (OV)
                    String createdDate = "NONE";
                    if (!fields.isNull("created")) {
                        createdDate = fields.getString("created").substring(0, 10);
                    }

                    // 2. Data Risoluzione (FV)
                    String resolvedDate = "NONE";
                    if (!fields.isNull("resolutiondate")) {
                        resolvedDate = fields.getString("resolutiondate").substring(0, 10);
                    }

                    // 3. Affected Versions (IV)
                    String versionsStr = "NONE";
                    if (!fields.isNull("versions")) {
                        JSONArray versionsArray = fields.getJSONArray("versions");
                        List<String> versionNames = new ArrayList<>();
                        for (int k = 0; k < versionsArray.length(); k++) {
                            versionNames.add(versionsArray.getJSONObject(k).getString("name"));
                        }
                        if (!versionNames.isEmpty()) {
                            // Usiamo ';' per evitare problemi col CSV
                            versionsStr = String.join(";", versionNames);
                        }
                    }

                    // Scrivi la riga nel file CSV
                    writer.printf("%s,%s,%s,%s%n", key, createdDate, resolvedDate, versionsStr);
                }

                System.out.println("Scaricati " + i + " ticket su " + total + "...");

            } while (i < total);

            System.out.println("\nCompletato! File salvato in: " + outputFile);
        }
    }
}
