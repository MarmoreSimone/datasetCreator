package entity;

public class ReleaseInfo {
    private String releaseIndex;//index
    private String releaseID;//version name es. 1.2.3
    private String date;//date

    public ReleaseInfo(String releaseIndex, String releaseID, String date) {
        this.releaseIndex = releaseIndex;
        this.releaseID = releaseID;
        this.date = date;
    }

    public String getReleaseIndex() {
        return releaseIndex;
    }

    public String getReleaseID() {
        return releaseID;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "ReleaseID: " + releaseIndex + " | Name: " + releaseID + " | Date: " + date;
    }
}
