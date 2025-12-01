package ast;

// RealLiteral - represents a real number literal in expressions
public class RealLiteral extends Expression {
    public final double value;

    public RealLiteral(double value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}