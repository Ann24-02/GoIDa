package ast;

import java.util.List;     // для списков
import parser.Token;       // для Token.Type


/**
 * Корневой узел - вся программа
 */
public class Program extends ASTNode {
    public final List<Declaration> declarations;
    
    public Program(List<Declaration> declarations, int line, int column) {
        super(line, column);
        this.declarations = declarations;
    }
}