package ast;


/**
 * Базовый класс для объявлений
 */
public abstract class Declaration extends ASTNode {
    public final String name;
    
    public Declaration(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }
}
