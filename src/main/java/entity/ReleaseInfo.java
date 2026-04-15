package entity;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ReleaseInfo {
    private String releaseID;//index
    private String releaseName;//version name
    private String date;//date

    public ReleaseInfo(String releaseID, String releaseName, String date) {
        this.releaseID = releaseID;
        this.releaseName = releaseName;
        this.date = date;
    }

    public String getReleaseID() {
        return releaseID;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "ReleaseID: " + releaseID + " | Name: " + releaseName + " | Date: " + date;
    }
}
