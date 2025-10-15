package ast;

/**
 * Базовый класс для типов
 */
public abstract class Type extends ASTNode {
    public Type(int line, int column) {
        super(line, column);
    }
}

