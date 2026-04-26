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
    private int churnTotal, churnPartial;
    private int maxChurnTotal, maxChurnPartial;
    private int avgChurnTotal, avgChurnPartial;
    private int age;
    private int chgSetTotal, chgSetPartial;
    private int maxChgSetTotal, maxChgSetPartial;
    private int avgChangeSetTotal, avgChangeSetPartial;

    private String buggy = "no"; // Valore di default

    public ClassMetrics(String filePath, String releaseID, String version) {
        this.filePath = filePath;
        this.releaseID = releaseID;
        this.version = version;
    }

    //setter
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

    public void setChurnTotal(int churnTotal) {
        this.churnTotal = churnTotal;
    }

    public void setChurnPartial(int churnPartial) {
        this.churnPartial = churnPartial;
    }

    public void setMaxChurnTotal(int maxChurnTotal) {
        this.maxChurnTotal = maxChurnTotal;
    }

    public void setMaxChurnPartial(int maxChurnPartial) {
        this.maxChurnPartial = maxChurnPartial;
    }

    public void setAvgChurnPartial(int avgChurnPartial) {
        this.avgChurnPartial = avgChurnPartial;
    }

    public void setAvgChurnTotal(int avgChurnTotal) {
        this.avgChurnTotal = avgChurnTotal;
    }

    public void setAge(int age){ this.age = age; }

    public void setChgSetTotal(int chgSetTotal) {
        this.chgSetTotal = chgSetTotal;
    }

    public void setChgSetPartial(int chgSetPartial) {
        this.chgSetPartial = chgSetPartial;
    }

    public void setMaxChgSetTotal(int maxChgSetTotal) {
        this.maxChgSetTotal = maxChgSetTotal;
    }

    public void setMaxChgSetPartial(int maxChgSetPartial) {
        this.maxChgSetPartial = maxChgSetPartial;
    }

    public void setAvgChgSetTotal(int avgChangeSetTotal) {
        this.avgChangeSetTotal = avgChangeSetTotal;
    }

    public void setAvgChgSetPartial(int avgChangeSetPartial) {
        this.avgChangeSetPartial = avgChangeSetPartial;
    }

    //getter
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

    public int getChurnTotal() {
        return churnTotal;
    }

    public int getChurnPartial() {
        return churnPartial;
    }

    public int getMaxChurnTotal() {
        return maxChurnTotal;
    }

    public int getMaxChurnPartial() {
        return maxChurnPartial;
    }

    public int getAvgChurnPartial() {
        return avgChurnPartial;
    }

    public int getAvgChurnTotal() {
        return avgChurnTotal;
    }

    public int getAge() {
        return age;
    }

    public int getAvgChangeSetPartial() {
        return avgChangeSetPartial;
    }

    public int getAvgChangeSetTotal() {
        return avgChangeSetTotal;
    }

    public int getChgSetPartial() {
        return chgSetPartial;
    }

    public int getChgSetTotal() {
        return chgSetTotal;
    }

    public int getMaxChgSetPartial() {
        return maxChgSetPartial;
    }

    public int getMaxChgSetTotal() {
        return maxChgSetTotal;
    }

    public String getBuggy() {
        return buggy;
    }

    public void setBuggy() {
        this.buggy = "yes";
    }

    public String toCsvRow() {
        return String.format("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s",
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
                churnTotal,
                churnPartial,
                maxChurnTotal,
                maxChurnPartial,
                avgChurnTotal,
                avgChurnPartial,
                age,
                chgSetTotal,
                chgSetPartial,
                maxChgSetTotal,
                maxChgSetPartial,
                avgChangeSetTotal,
                avgChangeSetPartial,
                buggy
        );
    }

    //togliere, serve per il test
    //todo
    private String predecessorID;
    public String getPredecessorID() {
        return predecessorID;
    }
    public void setPredecessorID(String predecessorID) {
        this.predecessorID = predecessorID;
    }

    public void setAllMetrics(int nrTotal, int nrPartial,
                              int nFixTotal, int nFixPartial,
                              int nAuthTotal, int nAuthPartial,
                              int locAddedTotal, int locAddedPartial,
                              int churnTotal, int churnPartial,
                              int maxChurnTotal, int maxChurnPartial,
                              int avgChurnTotal, int avgChurnPartial,
                              int age,
                              int chgSetTotal, int chgSetPartial,
                              int maxChgSetTotal, int maxChgSetPartial,
                              int avgChangeSetTotal, int avgChangeSetPartial) {
        this.nrTotal = nrTotal;
        this.nrPartial = nrPartial;
        this.nFixTotal = nFixTotal;
        this.nFixPartial = nFixPartial;
        this.nAuthTotal = nAuthTotal;
        this.nAuthPartial = nAuthPartial;
        this.locAddedTotal = locAddedTotal;
        this.locAddedPartial = locAddedPartial;
        this.churnTotal = churnTotal;
        this.churnPartial = churnPartial;
        this.maxChurnTotal = maxChurnTotal;
        this.maxChurnPartial = maxChurnPartial;
        this.avgChurnTotal = avgChurnTotal;
        this.avgChurnPartial = avgChurnPartial;
        this.age = age;
        this.chgSetTotal = chgSetTotal;
        this.chgSetPartial = chgSetPartial;
        this.maxChgSetTotal = maxChgSetTotal;
        this.maxChgSetPartial = maxChgSetPartial;
        this.avgChangeSetTotal = avgChangeSetTotal;
        this.avgChangeSetPartial = avgChangeSetPartial;
    }
}