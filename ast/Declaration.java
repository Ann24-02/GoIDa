package ast;

// Declaration - base class for all declarations
public abstract class Declaration extends ASTNode {
    public final String name;
    
    public Declaration(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }
}