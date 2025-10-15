package ast;
import parser.Token;

public class Identifier extends Expression {
    public final String name;

    public Identifier(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }
}
