package ast;
import parser.Token;

// BinaryExpression - represents a binary expression with left and right operands and an operator
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