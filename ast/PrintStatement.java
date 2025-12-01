package ast;
import java.util.List;

// PrintStatement - represents a print statement with a list of expressions to print
public class PrintStatement extends Statement {
    public final List<Expression> expressions;

    public PrintStatement(List<Expression> expressions, int line, int column) {
        super(line, column);
        this.expressions = expressions;
    }
}