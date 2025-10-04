package ast;

/**
 * Параметр подпрограммы: x : integer
 */
public class Parameter extends ASTNode {
    public final String name;
    public final Type type;
    
    public Parameter(String name, Type type, int line, int column) {
        super(line, column);
        this.name = name;
        this.type = type;
    }
}