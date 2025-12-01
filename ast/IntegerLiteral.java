package ast;

// IntegerLiteral - represents an integer literal in expressions
public class IntegerLiteral extends Expression {
    public final int value;

    public IntegerLiteral(int value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}