package ast;

import java.util.List;     // для списков
import parser.Token;       // для Token.Type


/**
 * Тело подпрограммы или блока: смесь объявлений и инструкций
 */
public class Body extends ASTNode {
    public final List<ASTNode> elements; // Declaration или Statement
    
    public Body(List<ASTNode> elements, int line, int column) {
        super(line, column);
        this.elements = elements;
    }
}
