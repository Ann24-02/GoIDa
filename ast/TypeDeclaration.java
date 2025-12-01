package ast;

// TypeDeclaration - represents a type declaration that aliases another type
public class TypeDeclaration extends Declaration {
    public final Type aliasedType;

    public TypeDeclaration(String name, Type aliasedType, int line, int column) {
        super(name, line, column);
        this.aliasedType = aliasedType;
    }
}