package ast;
import parser.Token;

public class PrimitiveType extends Type {
    public final String typeName;

    public PrimitiveType(String typeName, int line, int column) {
        super(line, column);
        this.typeName = typeName;
    }
}
