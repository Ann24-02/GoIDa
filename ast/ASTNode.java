package ast;

/**
 * Базовый класс для всех узлов AST
 */
public abstract class ASTNode {
    public final int line;
    public final int column;

    public ASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }
}