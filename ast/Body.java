package ast;
import java.util.List;

// Body contains a list of elements
public class Body extends ASTNode {
    public final List<ASTNode> elements; // Declaration или Statement
    
    public Body(List<ASTNode> elements, int line, int column) {
        super(line, column);
        this.elements = elements;
    }
}