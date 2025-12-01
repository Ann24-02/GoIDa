package ast;

// ForLoop - represents a for loop with loop variable, range, direction, and body
public class ForLoop extends Statement {
    public final String loopVariable;
    public final Range range;
    public final boolean reverse;
    public final Body body;

    public ForLoop(String loopVariable, Range range, boolean reverse, Body body, int line, int column) {
        super(line, column);
        this.loopVariable = loopVariable;
        this.range = range;
        this.reverse = reverse;
        this.body = body;
    }
}