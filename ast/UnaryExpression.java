package ast;
import parser.Token;

public class UnaryExpression extends Expression {
    public final Token.Type operator;
    public final Expression operand;

    public UnaryExpression(Token.Type operator, Expression operand, int line, int column) {
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }
}
