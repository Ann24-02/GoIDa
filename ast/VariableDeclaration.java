package ast;

// VariableDeclaration - represents a variable declaration with type and optional initializer
public class VariableDeclaration extends Declaration {
    public final Type type;
    public final Expression initializer;

    public VariableDeclaration(String name, Type type, Expression initializer, int line, int column) {
        super(name, line, column);
        this.type = type;
        this.initializer = initializer;
    }
}