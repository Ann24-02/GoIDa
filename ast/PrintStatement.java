package ast;
import java.util.List;
import parser.Token;

public class PrintStatement extends Statement {
    public final List<Expression> expressions;

    public PrintStatement(List<Expression> expressions, int line, int column) {
        super(line, column);
        this.expressions = expressions;
    }
}
