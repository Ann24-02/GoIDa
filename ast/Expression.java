package ast;

import java.util.List;

/**
 * Базовый класс для выражений
 */
public abstract class Expression extends ASTNode {
    public Expression(int line, int column) {
        super(line, column);
    }
}

/**
 * Целочисленный литерал: 42
 */
class IntegerLiteral extends Expression {
    public final int value;
    
    public IntegerLiteral(int value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}

/**
 * Вещественный литерал: 3.14
 */
class RealLiteral extends Expression {
    public final double value;
    
    public RealLiteral(double value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}

/**
 * Булев литерал: true, false
 */
class BooleanLiteral extends Expression {
    public final boolean value;
    
    public BooleanLiteral(boolean value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}

/**
 * Строковый литерал: "hello"
 */
class StringLiteral extends Expression {
    public final String value;
    
    public StringLiteral(String value, int line, int column) {
        super(line, column);
        this.value = value;
    }
}

/**
 * Идентификатор: variableName
 */
class Identifier extends Expression {
    public final String name;
    
    public Identifier(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }
}

/**
 * Бинарная операция: left + right
 */
class BinaryExpression extends Expression {
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

/**
 * Унарная операция: -x, not flag
 */
class UnaryExpression extends Expression {
    public final Token.Type operator;
    public final Expression operand;
    
    public UnaryExpression(Token.Type operator, Expression operand, int line, int column) {
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }
}

/**
 * Вызов функции в выражении: max(a, b)
 */
class FunctionCall extends Expression {
    public final String functionName;
    public final List<Expression> arguments;
    
    public FunctionCall(String functionName, List<Expression> arguments, int line, int column) {
        super(line, column);
        this.functionName = functionName;
        this.arguments = arguments;
    }
}