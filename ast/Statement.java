package ast;

import java.util.List;

/**
 * Базовый класс для инструкций
 */
public abstract class Statement extends ASTNode {
    public Statement(int line, int column) {
        super(line, column);
    }
}

/**
 * Присваивание: x := 5
 */
class Assignment extends Statement {
    public final ModifiablePrimary target;
    public final Expression value;
    
    public Assignment(ModifiablePrimary target, Expression value, int line, int column) {
        super(line, column);
        this.target = target;
        this.value = value;
    }
}

/**
 * Вызов подпрограммы: print(x)
 */
class RoutineCall extends Statement {
    public final String routineName;
    public final List<Expression> arguments;
    
    public RoutineCall(String routineName, List<Expression> arguments, int line, int column) {
        super(line, column);
        this.routineName = routineName;
        this.arguments = arguments;
    }
}

/**
 * Цикл while: while x > 0 loop ... end
 */
class WhileLoop extends Statement {
    public final Expression condition;
    public final Body body;
    
    public WhileLoop(Expression condition, Body body, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }
}

/**
 * Цикл for: for i in 1..10 loop ... end
 */
class ForLoop extends Statement {
    public final String loopVariable;
    public final Range range;
    public final boolean reverse;
    public final Body body;
    
    public ForLoop(String loopVariable, Range range, boolean reverse, Body body, int line, int column) {
        super(line, column);
        this.loopVariable = loopVariable;
        this.range = range;
        this.reverse = reverse;
        this.body = body;
    }
}

/**
 * Условный оператор: if x > 0 then ... else ... end
 */
class IfStatement extends Statement {
    public final Expression condition;
    public final Body thenBranch;
    public final Body elseBranch;  // null если нет else
    
    public IfStatement(Expression condition, Body thenBranch, Body elseBranch, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}

/**
 * Инструкция вывода: print(x, y)
 */
class PrintStatement extends Statement {
    public final List<Expression> expressions;
    
    public PrintStatement(List<Expression> expressions, int line, int column) {
        super(line, column);
        this.expressions = expressions;
    }
}