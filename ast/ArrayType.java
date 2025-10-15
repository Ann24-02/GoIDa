package ast;
import parser.Token;

public class ArrayType extends Type {
    public final Expression size;
    public final Type elementType;

    public ArrayType(Expression size, Type elementType, int line, int column) {
        super(line, column);
        this.size = size;
        this.elementType = elementType;
    }
}
