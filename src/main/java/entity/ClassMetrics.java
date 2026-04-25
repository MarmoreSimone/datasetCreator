package entity;

public class ClassMetrics {

    private String filePath;
    private String releaseID;
    private String version;

    private int loc;
    private int nrTotal, nrPartial;
    private int nFixTotal, nFixPartial;
    private int nAuthTotal, nAuthPartial;
    private int locAddedTotal, locAddedPartial;

    private String buggy = "no"; // Valore di default

    public ClassMetrics(String filePath, String releaseID, String version) {
        this.filePath = filePath;
        this.releaseID = releaseID;
        this.version = version;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public void setNrTotal(int nrTotal) {
        this.nrTotal = nrTotal;
    }

    public void setNrPartial(int nrPartial){this.nrPartial = nrPartial; }

    public void setnFixTotal(int nfix){
        this.nFixTotal = nfix;
    }

    public void setnFixPartial(int nfix){
        this.nFixPartial = nfix;
    }

    public void setnAuthTotal(int nauth){ this.nAuthTotal = nauth; }

    public void setnAuthPartial(int nauth){ this.nAuthPartial = nauth; }

    public void setLocAddedTotal(int locAddedTotal) {
        this.locAddedTotal = locAddedTotal;
    }

    public void setLocAddedPartial(int locAddedPartial) {
        this.locAddedPartial = locAddedPartial;
    }

    public String getFilePath(){
        return this.filePath;
    }

    public String getReleaseID() {
        return releaseID;
    }

    public int getLoc(){
        return loc;
    }

    public int getNrTotal() {
        return this.nrTotal;
    }

    public int getNrPartial() {
        return nrPartial;
    }

    public int getnFixTotal() {
        return nFixTotal;
    }

    public int getnFixPartial() {
        return nFixPartial;
    }

    public int getnAuthTotal() {
        return nAuthTotal;
    }

    public int getnAuthPartial() {
        return nAuthPartial;
    }

    public int getLocAddedTotal() {
        return locAddedTotal;
    }

    public int getLocAddedPartial() {
        return locAddedPartial;
    }

    public String toCsvRow() {
        return String.format("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s",
                releaseID,
                version.equals("2.0.0") ? "2.0.0-beta4" : version, // <-- Modificato qui
                filePath,
                loc,
                nrTotal,
                nrPartial,
                nFixTotal,
                nFixPartial,
                nAuthTotal,
                nAuthPartial,
                locAddedTotal,
                locAddedPartial,
                buggy
        );
    }
}