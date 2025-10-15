package ast;
import java.util.List;
import parser.Token;

public class RoutineCall extends Statement {
    public final String routineName;
    public final List<Expression> arguments;

    public RoutineCall(String routineName, List<Expression> arguments, int line, int column) {
        super(line, column);
        this.routineName = routineName;
        this.arguments = arguments;
    }
}
