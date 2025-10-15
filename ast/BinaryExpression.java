package ast;
import parser.Token;

public class BinaryExpression extends Expression {
    public final Expression left;
    public final Token.Type operator;
    public final Expression right;

    public BinaryExpression(Expression left, Token.Type operator, Expression right, int line, int column) {
        super(line, column);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}
