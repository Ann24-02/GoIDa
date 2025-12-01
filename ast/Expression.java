package ast;

// Expression - base class for all expressions
public abstract class Expression extends ASTNode {
    public Expression(int line, int column) {
        super(line, column);
    }
}