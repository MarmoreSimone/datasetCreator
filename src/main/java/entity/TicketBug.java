package entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TicketBug {
    private String key;
    private LocalDate creationDate;
    private LocalDate resolutionDate;
    private List<String> affectedVersions;

    // Indici delle release che calcolerai nella fase successiva
    private int ov; // Opening Version
    private int fv; // Fixed Version
    private int iv; // Injected Version

    public TicketBug(String key, String creationDate, String resolutionDate, String affectedVersionsStr) {
        this.key = key;

        // Conversione stringa -> LocalDate per facilitare i confronti temporali
        this.creationDate = creationDate.equals("NONE") ? null : LocalDate.parse(creationDate);
        this.resolutionDate = resolutionDate.equals("NONE") ? null : LocalDate.parse(resolutionDate);

        this.affectedVersions = new ArrayList<>(); // Crei sempre una lista vera, mutabile e sicura

        if (affectedVersionsStr != null && !affectedVersionsStr.equals("NONE") && !affectedVersionsStr.isEmpty()) {
            String[] versions = affectedVersionsStr.split(";");
            for (String version : versions) {
                // Il trim() rimuove tutti gli spazi iniziali e finali (" 1.0.0 " diventa "1.0.0")
                this.affectedVersions.add(version.trim());
            }
        }
    }

    // Getters e Setters
    public String getKey() { return key; }
    public LocalDate getCreationDate() { return creationDate; }
    public LocalDate getResolutionDate() { return resolutionDate; }
    public List<String> getAffectedVersions() { return affectedVersions; }

    public int getOv() { return ov; }
    public void setOv(int ov) { this.ov = ov; }

    public int getFv() { return fv; }
    public void setFv(int fv) { this.fv = fv; }

    public int getIv() { return iv; }
    public void setIv(int iv) { this.iv = iv; }

    @Override
    public String toString() {
        return "TicketBug{" + "key='" + key + '\'' + ", affected=" + affectedVersions.size() + '}';
    }
}
