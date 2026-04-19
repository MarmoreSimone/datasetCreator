package entity;

public class ClassMetrics {

    private String className;
    private String releaseID;

    private int loc;
    private int nrTotal;
    private int nrPartial;
    private int nFixTotal;
    private int nFixPartial;
    private int nAuthTotal;
    private int nAuthPartial;

    private String buggy = "no"; // Valore di default

    public ClassMetrics(String className, String releaseID) {
        this.className = className;
        this.releaseID = releaseID;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public void setNrTotal(int nrTotal) {
        this.nrTotal = nrTotal;
    }

    public void setNrPartial(int nrPartial){this.nrPartial = nrPartial; }

    public void setnFixTotal(int Nfix){
        this.nFixTotal = Nfix;
    }

    public void setnFixPartial(int Nfix){
        this.nFixPartial = Nfix;
    }

    public void setnAuthTotal(int Nauth){ this.nAuthTotal = Nauth; }

    public void setnAuthPartial(int Nauth){ this.nAuthPartial = Nauth; }

    public String getClassName(){
        return this.className;
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

    public String toCsvRow() {
        // %d si usa per gli interi (LOC, NRtotal)
        return String.format("%s,%s,%d,%d,%d,%d,%d,%d,%d,%s",
                releaseID,
                className,
                loc,
                nrTotal,
                nrPartial,
                nFixTotal,
                nFixPartial,
                nAuthTotal,
                nAuthPartial,
                buggy
        );
    }
}