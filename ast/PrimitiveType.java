package ast;

// PrimitiveType - represents a primitive type (e.g., int, bool)
public class PrimitiveType extends Type {
    public final String typeName;

    public PrimitiveType(String typeName, int line, int column) {
        super(line, column);
        this.typeName = typeName;
    }
}