package acces_point;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// codice del falessi modificato per recuperare anche le altre informazioni del ticket
public class RetrieveTicketsID {

    private static final String PROJ_NAME = "OPENJPA";
    private static final String OUTPUT_FILE_PATH = "src/main/java/outputs/jiraTicketsEnriched.csv";
    private static final int PAGE_SIZE = 1000;

    static void main() throws IOException, JSONException {
        String outputFile = OUTPUT_FILE_PATH;

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println("TicketID,CreationDate,ResolutionDate,AffectedVersions");
            fetchAndWriteIssues(writer);
            System.out.printf("%nfile salvato in: %s%n", outputFile);
        }
    }

    private static void fetchAndWriteIssues(PrintWriter writer) throws IOException, JSONException {
        int i = 0;
        int total;
        do {
            int maxResults = i + PAGE_SIZE;
            String url = String.format(
                    "https://issues.apache.org/jira/rest/api/2/search?jql=project=%%22%s%%22AND%%22issueType%%22=%%22Bug%%22AND(%%22status%%22=%%22closed%%22OR%%22status%%22=%%22resolved%%22)AND%%22resolution%%22=%%22fixed%%22&fields=key,resolutiondate,versions,created&startAt=%d&maxResults=%d",
                    PROJ_NAME, i, maxResults);

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            for (int k = 0; k < issues.length() && i < total; k++, i++) {
                processAndWriteIssue(writer, issues.getJSONObject(k));
            }

            System.out.printf("scaricati %d ticket su %d%n", i, total);
        } while (i < total);
    }

    private static void processAndWriteIssue(PrintWriter writer, JSONObject issue) throws JSONException {
        String key = issue.getString("key");
        JSONObject fields = issue.getJSONObject("fields");

        String createdDate = fields.isNull("created") ? "NONE"
                : fields.getString("created").substring(0, 10);

        String resolvedDate = fields.isNull("resolutiondate") ? "NONE"
                : fields.getString("resolutiondate").substring(0, 10);

        String versionsStr = getVersionsString(fields);

        writer.printf("%s,%s,%s,%s%n", key, createdDate, resolvedDate, versionsStr);
    }

    private static String getVersionsString(JSONObject fields) throws JSONException {
        if (fields.isNull("versions")) {
            return "NONE";
        }

        JSONArray versionsArray = fields.getJSONArray("versions");
        if (versionsArray.length() == 0) {
            return "NONE";
        }

        List<String> versionNames = new ArrayList<>();
        for (int k = 0; k < versionsArray.length(); k++) {
            versionNames.add(versionsArray.getJSONObject(k).getString("name"));
        }

        return String.join(";", versionNames);
    }

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
}