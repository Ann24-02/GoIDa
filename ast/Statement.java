package ast;


/**
 * Базовый класс для инструкций
 */
public abstract class Statement extends ASTNode {
    public Statement(int line, int column) {
        super(line, column);
    }
}