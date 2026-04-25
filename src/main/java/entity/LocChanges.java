package entity;

public class LocChanges {

    private int added;
    private int deleted;
    private int modified;

    public LocChanges(int added, int deleted, int modified){
        this.added = added;
        this.modified = modified;
        this. deleted = deleted;
    }

    public int getLocAdded(){
        return (this.added + this.modified);
    }

    public int getChurn(){
        return (this.added + this.modified + this.deleted);
    }

}
