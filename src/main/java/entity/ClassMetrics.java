package entity;

public class ClassMetrics {

    private String className;
    private String releaseID;

    private int LOC;
    private int NRtotal;

    private String buggy = "no"; // Valore di default

    public ClassMetrics(String className, String releaseID) {
        this.className = className;
        this.releaseID = releaseID;
    }

    public void setLOC(int LOC) {
        this.LOC = LOC;
    }

    public void setNRtotal(int NRtotal) {
        this.NRtotal = NRtotal;
    }

    public String getClassName(){
        return this.className;
    }

    public String getReleaseID() {
        return releaseID;
    }

    public int getLOC(){
        return LOC;
    }

    public int getNRtotal() {
        return this.NRtotal;
    }

    public String toCsvRow() {
        // %s indica una Stringa, %d indica un numero intero (digit)
        return String.format("%s,%s,%s",
                className,
                releaseID,
                buggy
        );
    }
}