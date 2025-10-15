package ast;
import parser.Token;

public class WhileLoop extends Statement {
    public final Expression condition;
    public final Body body;

    public WhileLoop(Expression condition, Body body, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }
}
