package ast;
import java.util.List;


// Function call expression
// Also used for array and record literals internally by parser.

public class FunctionCall extends Expression {
    public final String functionName;
    public final List<Expression> arguments;

    public FunctionCall(String functionName, List<Expression> arguments, int line, int column) {
        super(line, column);
        this.functionName = functionName;
        this.arguments = arguments;
    }
}