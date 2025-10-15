package ast;
/**
 * Базовый класс для выражений
 */
public abstract class Expression extends ASTNode {
    public Expression(int line, int column) {
        super(line, column);
    }
}