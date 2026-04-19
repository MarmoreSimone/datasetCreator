package entity;

public class ClassMetrics {

    private String className;
    private String releaseID;

    private int LOC;
    private int NRtotal;
    private int NRpartial;
    private int NfixTotal;
    private int NfixPartial;
    private int NauthTotal;
    private int NauthPartial;

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

    public void setNRpartial(int NRpartial){this.NRpartial = NRpartial; }

    public void setNfixTotal(int Nfix){
        this.NfixTotal = Nfix;
    }

    public void setNfixPartial(int Nfix){
        this.NfixPartial= Nfix;
    }

    public void setNauthTotal(int Nauth){ this.NauthTotal = Nauth; }

    public void setNauthPartial(int Nauth){ this.NauthPartial = Nauth; }

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

    public int getNRpartial() {
        return NRpartial;
    }

    public int getNfixTotal() {
        return NfixTotal;
    }

    public int getNfixPartial() {
        return NfixPartial;
    }

    public int getNauthTotal() {
        return NauthTotal;
    }

    public int getNauthPartial() {
        return NauthPartial;
    }

    public String toCsvRow() {
        // %d si usa per gli interi (LOC, NRtotal)
        return String.format("%s,%s,%d,%d,%d,%d,%d,%d,%d,%s",
                releaseID,
                className,
                LOC,
                NRtotal,
                NRpartial,
                NfixTotal,
                NfixPartial,
                NauthTotal,
                NauthPartial,
                buggy
        );
    }
}