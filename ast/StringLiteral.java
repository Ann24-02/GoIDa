package ast;

// StringLiteral - represents a string literal in expressions
public class StringLiteral extends Expression {
    public final String value;

    public StringLiteral(String value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}