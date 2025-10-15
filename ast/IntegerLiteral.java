package ast;
import parser.Token;

public class IntegerLiteral extends Expression {
    public final int value;

    public IntegerLiteral(int value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}
