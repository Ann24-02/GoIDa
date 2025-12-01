package ast;
import java.util.List;

// RoutineDeclaration - represents a routine (procedure or function) declaration
public class RoutineDeclaration extends Declaration {
    public final List<Parameter> parameters;
    public final Type returnType;
    public final Body body;
    public final Expression expressionBody;

    public RoutineDeclaration(String name, List<Parameter> parameters, Type returnType, Body body, Expression expressionBody, int line, int column) {
        super(name, line, column);
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
        this.expressionBody = expressionBody;
    }
}