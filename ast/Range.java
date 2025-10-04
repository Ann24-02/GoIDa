package ast;

/**
 * Диапазон для цикла for: 1..10
 */
public class Range extends ASTNode {
    public final Expression start;
    public final Expression end;
    
    public Range(Expression start, Expression end, int line, int column) {
        super(line, column);
        this.start = start;
        this.end = end;
    }
}