package ast;


import java.util.List;     // для списков
import parser.Token;       // для Token.Type


/**
 * Базовый класс для объявлений
 */
public abstract class Declaration extends ASTNode {
    public final String name;
    
    public Declaration(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }
}

/**
 * Объявление переменной: var x : integer is 5
 */
class VariableDeclaration extends Declaration {
    public final Type type;          // null если есть инициализатор
    public final Expression initializer; // null если нет инициализатора
    
    public VariableDeclaration(String name, Type type, Expression initializer, int line, int column) {
        super(name, line, column);
        this.type = type;
        this.initializer = initializer;
    }
}

/**
 * Объявление типа: type MyInt is integer
 */
class TypeDeclaration extends Declaration {
    public final Type aliasedType;
    
    public TypeDeclaration(String name, Type aliasedType, int line, int column) {
        super(name, line, column);
        this.aliasedType = aliasedType;
    }
}

/**
 * Объявление подпрограммы: routine main() is ... end
 */
class RoutineDeclaration extends Declaration {
    public final List<Parameter> parameters;
    public final Type returnType;  // null для процедур
    public final Body body;        // null для forward declaration
    public final Expression expressionBody; // Альтернативная форма с ->
    
    public RoutineDeclaration(String name, List<Parameter> parameters, 
                            Type returnType, Body body, Expression expressionBody,
                            int line, int column) {
        super(name, line, column);
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
        this.expressionBody = expressionBody;
    }
}