package ast;
import java.util.List;

// Program - represents the entire program with a list of declarations
public class Program extends ASTNode {
    public final List<Declaration> declarations;
    
    public Program(List<Declaration> declarations, int line, int column) {
        super(line, column);
        this.declarations = declarations;
    }
}