package ast;
import parser.Token;

public class BooleanLiteral extends Expression {
    public final boolean value;

    public BooleanLiteral(boolean value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}
