package ast;
import parser.Token;

public class IfStatement extends Statement {
    public final Expression condition;
    public final Body thenBranch;
    public final Body elseBranch;

    public IfStatement(Expression condition, Body thenBranch, Body elseBranch, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}
