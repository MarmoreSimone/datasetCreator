package entity;

// usata per la milestone 4
public class SmellInfo {

    private final String classPath;
    private final String ruleName;
    private final int priority;
    private final String description;
    private final int startLine;
    private final int endLine;

    public SmellInfo(String classPath, String ruleName, int priority, String description, int startline, int endline) {
        this.classPath = classPath;
        this.ruleName = ruleName;
        this.priority = priority;
        this.description = description;
        this.startLine = startline;
        this.endLine = endline;
    }

    public String toCsvRow() {
        return String.format("%s,%s,%d,\"%s\",%d,%d",
                classPath,
                ruleName,
                priority,
                description,
                startLine,
                endLine
        );
    }
}
