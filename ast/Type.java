package ast;

// Type - base class for all types
public abstract class Type extends ASTNode {
    public Type(int line, int column) {
        super(line, column);
    }
}