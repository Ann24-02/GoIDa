package ast;

// Statement - base class for all statements
public abstract class Statement extends ASTNode {
    public Statement(int line, int column) {
        super(line, column);
    }
}