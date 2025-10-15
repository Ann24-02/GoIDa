package ast;
import parser.Token;

public class RealLiteral extends Expression {
    public final double value;

    public RealLiteral(double value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}
